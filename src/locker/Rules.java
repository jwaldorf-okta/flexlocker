/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package locker;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

/**
 *
 * @author jwaldorf
 */
public class Rules {

    Rule[] rule = new Rule[0];

    void load(String file) throws FileNotFoundException, IOException {
        LinkedList<Rule> ruleList = new LinkedList<Rule>();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = br.readLine();
        while (line != null) {
            int v = 0;
            String[] values = new String[4];
            values[0] = "";
            values[1] = "";
            values[2] = "";
            values[3] = "";
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == ',') {
                    v++;
                } else if (c == '/') {
                    i++;
                    values[v] += line.charAt(i);
                } else {
                    values[v] += c;
                }
            }
            ruleList.add(new Rule(values[0], values[1], values[2], values[3]));
            line = br.readLine();
        }
        br.close();
        rule = ruleList.toArray(rule);
    }
}