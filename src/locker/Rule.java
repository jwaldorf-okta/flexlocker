/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package locker;

/**
 *
 * @author jwaldorf
 */
public class Rule {
    String group;
    String application;
    String attributeName;
    String attributeValue;
    
    public Rule(String grp, String app, String attN, String attV) {
        group = grp;
        application = app;
        attributeName = attN;
        attributeValue = attV;
    }
}
