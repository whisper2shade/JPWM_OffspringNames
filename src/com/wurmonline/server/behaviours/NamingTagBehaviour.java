/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wurmonline.server.behaviours;

import com.jpiolho.wurmmod.offspringnames.ModOffspringNames;
import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureStatus;
import com.wurmonline.server.creatures.CreatureTemplateIds;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.creatures.ModOffspringNamesCreaturesProxy;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.questions.CarvingNameQuestion;
import com.wurmonline.server.skills.SkillList;
import com.wurmonline.server.zones.Zone;
import com.wurmonline.server.zones.Zones;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author JPiolho
 */
public class NamingTagBehaviour extends ItemBehaviour {

    public static short ID;
    
    public NamingTagBehaviour() {
        super(ID = BehaviourUtils.GetNextBehaviourType());
    }

    List<ActionEntry> getNamingTagBehaviours(Creature performer,Item source,Item target) {
        LinkedList<ActionEntry> toReturn = new LinkedList<>();
        
        if(source != null) {
            toReturn.add(ModOffspringNames.actionCarveName);
        }
        
        return toReturn;
    }
    
    @Override
    List<ActionEntry> getBehavioursFor(Creature performer, Item source, Item target) {
        List<ActionEntry> toReturn = super.getBehavioursFor(performer, source, target);
        
        for(ActionEntry action : toReturn) {
            if(action.getActionString().equals("Rename"))
            {
                toReturn.remove(action);
                break;
            }
        }
        
        toReturn.addAll(getNamingTagBehaviours(performer, source, target));
        return toReturn;
    }

    
    
    
    
    boolean performNamingTagAction(Action aAct, Creature aPerformer, Item aSource, Item aTarget, short aAction, float aCounter) {
        
        if(aAction == ModOffspringNames.actionCarveName.getNumber()) {
            if(aSource == null) {
                aPerformer.getCommunicator().sendNormalServerMessage("You fumble with the " + aTarget + " but you cannot figure out how it works.");
                return true;
            }
            else if(aPerformer.getPower() < 2 && aSource.getTemplateId() != ItemList.knifeCarving) {
                aPerformer.getCommunicator().sendNormalServerMessage("You damage the item trying to carve it with the " + aSource.getActualName() + ".");
                aTarget.setDamage(aTarget.getDamage() + 0.001f * aTarget.getDamageModifier());
                return true;
            }
            else if(aTarget.getDescription().length() > 0) {
                aPerformer.getCommunicator().sendNormalServerMessage("This tag is already carved with a name.");
                return true;
            }
            
            CarvingNameQuestion question = new CarvingNameQuestion(aPerformer, aTarget.getWurmId(), GetMaximumCharacters(aTarget));
            question.sendQuestion();
            return true;
        }        
        
        return false;
    }

    @Override
    boolean action(Action act, Creature performer, Item target, short action, float counter) {
        if(!this.performNamingTagAction(act, performer, null, target, action, counter))
            return super.action(act, performer, target, action, counter);
        return true;
    }

    @Override
    boolean action(Action act, Creature performer, Item source, Item target, short action, float counter) {
        if(!this.performNamingTagAction(act, performer, source, target, action, counter))
            return super.action(act, performer, source, target, action,counter);
        return true;
    }
    
    
    @Override
    public boolean action(Action action, Creature performer, Item source, Creature target, short num, float counter) {
        
        if(source.getTemplateId() == ModOffspringNames.iid_namingtag) {
            if(source.getDescription().length() == 0) {
                performer.getCommunicator().sendNormalServerMessage("This tag isn't carved yet.");
                return true;
            }
            
            if(target.getTemplate().getTemplateId() != CreatureTemplateIds.HORSE_CID) {
                performer.getCommunicator().sendNormalServerMessage("You can only use this tag on horses.");
                return true;
            }
            
            if(!target.getNameWithoutPrefixes().equalsIgnoreCase(target.getTemplate().getName())) {
                performer.getCommunicator().sendNormalServerMessage("This animal has already been tagged.");
                return true;
            }
            
            String previousName = target.getNameWithoutPrefixes();
            String newName = source.getDescription();
            try {
                target.setName(newName);
                target.save();
                ModOffspringNamesCreaturesProxy.SaveCreatureName(target.getStatus(), newName);
                
                // Update the creature name to everyone
                Zone zone = Zones.getZone(target.getTileX(), target.getTileY(), target.isOnSurface());
                zone.removeCreature(target, true, false);
                zone.addCreature(target.getWurmId());
                
                Items.destroyItem(source.getWurmId());
            } catch(Exception ex) {
                performer.getCommunicator().sendNormalServerMessage("Something went wrong, you can't seem to tag this animal.");
                return true;
            }
            
            performer.getCommunicator().sendNormalServerMessage("You tag the " + previousName + ".");
            
            
            
        }
        
        
        return super.action(action, performer, source, target, num, counter); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
    public static int GetMaximumCharacters(Item item) {
        int chars = (int)Math.ceil(20 * (item.getQualityLevel() / 100f));
        return chars < 3 ? 3 : chars;
    }
}
