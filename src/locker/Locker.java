/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package locker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jwaldorf
 */
public class Locker {

    static Proxy proxy;
    static String lastUpdated = "2014-05-06T17:20:01.000Z";
    static boolean first = true;

    static JSONArray OktaGetAll(String urlPrefix, String token) throws MalformedURLException {
        int count = 0;
        String currentLastUpdated = "";
        URL url;
        HttpURLConnection conn;
        JSONArray result = new JSONArray();

        if (lastUpdated.length() > 0) {
            String tmp = lastUpdated;
            DateFormat dateFormat;
            if (lastUpdated.endsWith("Z")) {
                dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            } else {
                dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
            }
            try {
                Date date = dateFormat.parse(lastUpdated);
                tmp = dateFormat.format(date);
                url = new URL(urlPrefix + "/events" + "?filter=published+gt+%22" + tmp + "%22"
                        + "+and+(+action.objectType+eq+%22core.user_group_member.user_add%22+or+action.objectType+eq+%22app.user_management.app_group_member_import.insert_success%22+or+action.objectType+eq+%22app.user_management.app_group_member_import.delete_success%22+or+action.objectType+eq+%22core.user_group_member.user_remove%22)"
                        + "&limit=200");
            } catch (Exception e) {
                e.printStackTrace();
//                sendEmail("Rules Engine Error", new Date() + " " + e.getLocalizedMessage());
                url = new URL(urlPrefix + "/events" + "?filter=(+action.objectType+eq+%22core.user_group_member.user_add%22+or+action.objectType+eq+%22app.user_management.app_group_member_import.insert_success%22+or+action.objectType+eq+%22app.user_management.app_group_member_import.delete_success%22+or+action.objectType+eq+%22core.user_group_member.user_remove%22)"
                        + "&limit=200");
            }
        } else {
            url = new URL(urlPrefix + "/events" + "?limit=200");
        }
        while (true) {
//            System.out.println("GET " + url);
            try {
                if (proxy != null) {
                    conn = (HttpURLConnection) url.openConnection(proxy);
                } else {
                    conn = (HttpURLConnection) url.openConnection();
                }
                conn.setConnectTimeout(100000);
                conn.setReadTimeout(100000);
                conn.setRequestProperty("Authorization", "SSWS " + token);
                conn.setRequestMethod("GET");
                int retVal = conn.getResponseCode();
                if (retVal == 200) {
                    JSONArray sArray = new JSONArray(new JSONTokener(conn.getInputStream()));
                    for (int i = 0; i < sArray.length(); i++) {
                        String tLastUpdated;
                        tLastUpdated = sArray.getJSONObject(i).getString("published");
                        if (tLastUpdated.compareTo(currentLastUpdated) > 0) {
                            currentLastUpdated = tLastUpdated;
                        }
                        result.put(sArray.getJSONObject(i));
                        count++;
                    }
                    if (conn.getInputStream() != null) {
                        conn.getInputStream().close();
                    }
                    Map<String, List<String>> map = conn.getHeaderFields();
                    String link;
                    if (map.containsKey("Link")) {
                        List<String> l = map.get("Link");
                        boolean found = false;
                        for (int i = 0; i < l.size(); i++) {
                            String val = map.get("Link").get(i);
                            String[] pair = val.split(";");
                            if (pair[1].contains("next")) {
                                link = pair[0].substring(1, pair[0].length() - 1);
                                url = new URL(link);
                                found = true;
                                break;
                            }
                        }
                        if (found == false) {
                            break;
                        }
                    } else {
                        break;
                    }
                } else {
                    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    String line;
                    String res = "";
                    while ((line = rd.readLine()) != null) {
                        res += line;
                    }
                    System.out.println(new Date() + " GET " + url.toString() + " RETURNS " + conn.getResponseCode() + ":" + conn.getResponseMessage());
                    System.out.println(new Date() + " ERRORSTREAM = " + res);
                    rd.close();
//                sendEmail("Rules Engine Error", new Date() + " " + conn.getResponseMessage());
                    // break;
                }
            } catch (Exception e) {
                System.out.println(new Date() + " GET " + url.toString());
                e.printStackTrace();
            }
        }
        if (currentLastUpdated.length() > 0) {
            lastUpdated = currentLastUpdated;
        }
//        System.out.println("" + count + " Events received.");
        return result;
    }

    static JSONArray OktaGetAllUsers(String urlPrefix, String group, String token) throws IOException {
        int count = 0;
        URL url;
        HttpURLConnection conn;
        JSONArray result = new JSONArray();

        url = new URL(urlPrefix + "/groups/" + group + "/users" + "?limit=200");
        while (true) {
            System.out.println(new Date() + " GET " + url);
            if (proxy != null) {
                conn = (HttpURLConnection) url.openConnection(proxy);
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }
            conn.setConnectTimeout(100000);
            conn.setReadTimeout(100000);
            conn.setRequestProperty("Authorization", "SSWS " + token);
            conn.setRequestMethod("GET");
            int retVal = conn.getResponseCode();
            if (retVal == 200) {
                JSONArray sArray = new JSONArray(new JSONTokener(conn.getInputStream()));
                for (int i = 0; i < sArray.length(); i++) {
                    result.put(sArray.getJSONObject(i));
                    count++;
                }
                if (conn.getInputStream() != null) {
                    conn.getInputStream().close();
                }
                Map<String, List<String>> map = conn.getHeaderFields();
                String link;
                if (map.containsKey("Link")) {
                    List<String> l = map.get("Link");
                    boolean found = false;
                    for (int i = 0; i < l.size(); i++) {
                        String val = map.get("Link").get(i);
                        String[] pair = val.split(";");
                        if (pair[1].contains("next")) {
                            link = pair[0].substring(1, pair[0].length() - 1);
                            url = new URL(link);
                            found = true;
                            break;
                        }
                    }
                    if (found == false) {
                        break;
                    }
                } else {
                    break;
                }
            } else {
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String line;
                String res = "";
                while ((line = rd.readLine()) != null) {
                    res += line;
                }
                System.out.println(new Date() + " GET " + url.toString() + " RETURNS " + conn.getResponseCode() + ":" + conn.getResponseMessage());
                System.out.println(new Date() + " ERRORSTREAM = " + res);
                rd.close();
//                sendEmail("Rules Engine Error", new Date() + " " + conn.getResponseMessage());
                break;
            }
        }
        System.out.println(new Date() + " " + count + " Users received.");
        return result;
    }

    public static HashSet<String> getUserGroups(String user, String urlPrefix, String token) throws MalformedURLException, IOException {
        HashSet<String> ret = new HashSet<String>();
        int count = 0;
        boolean retry = true;
        while (retry) {
            retry = false;
            ret = new HashSet<String>();
            count = 0;
            try {
                URL url = new URL(urlPrefix + "/users/" + user + "/groups");
                HttpURLConnection conn;
                while (true) {
                    if (proxy != null) {
                        conn = (HttpURLConnection) url.openConnection(proxy);
                    } else {
                        conn = (HttpURLConnection) url.openConnection();
                    }
                    conn.setConnectTimeout(100000);
                    conn.setReadTimeout(100000);
                    conn.setRequestProperty("Authorization", "SSWS " + token);
                    conn.setRequestMethod("GET");
                    int retVal = conn.getResponseCode();
                    if (retVal == 200) {
                        JSONArray sArray = new JSONArray(new JSONTokener(conn.getInputStream()));
                        for (int i = 0; i < sArray.length(); i++) {
                            ret.add(sArray.getJSONObject(i).getJSONObject("profile").getString("name"));
                            count++;
                        }
                        if (conn.getInputStream() != null) {
                            conn.getInputStream().close();
                        }
                        Map<String, List<String>> map = conn.getHeaderFields();
                        String link;
                        if (map.containsKey("Link")) {
                            List<String> l = map.get("Link");
                            boolean found = false;
                            for (int i = 0; i < l.size(); i++) {
                                String val = map.get("Link").get(i);
                                String[] pair = val.split(";");
                                if (pair[1].contains("next")) {
                                    link = pair[0].substring(1, pair[0].length() - 1);
                                    url = new URL(link);
                                    found = true;
                                    break;
                                }
                            }
                            if (found == false) {
                                break;
                            }
                        } else {
                            break;
                        }
                    } else if (retVal == 404) {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        String line;
                        String res = "";
                        while ((line = rd.readLine()) != null) {
                            res += line;
                        }
                        System.out.println(new Date() + " GET " + url.toString() + " RETURNS " + conn.getResponseCode() + ":" + conn.getResponseMessage());
                        System.out.println(new Date() + " ERRORSTREAM = " + res);
                        rd.close();
                        ret = null;
//                sendEmail("Rules Engine Error getting Groups for " + user, new Date() + " " + conn.getResponseMessage());
                        retry = true;
                        break;
                    } else {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        String line;
                        String res = "";
                        while ((line = rd.readLine()) != null) {
                            res += line;
                        }
                        System.out.println(new Date() + " GET " + url.toString() + " RETURNS " + conn.getResponseCode() + ":" + conn.getResponseMessage());
                        System.out.println(new Date() + " ERRORSTREAM = " + res);
                        rd.close();
//                sendEmail("Rules Engine Error getting Groups for " + user, new Date() + " " + conn.getResponseMessage());
                        retry = true;
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println(new Date() + " GET " + urlPrefix + "/users/" + user + "/groups" + " EXCEPTION");
                e.printStackTrace();
                retry = true;
            }
        }
        return ret;
    }

    public static String get(String resource, String token) {
        boolean tryAgain = true;
        String result = "";
        URL url;
        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        try {
            url = new URL(resource);
            while (tryAgain) {
                result = "";
                try {
                    if (proxy != null) {
                        conn = (HttpURLConnection) url.openConnection(proxy);
                    } else {
                        conn = (HttpURLConnection) url.openConnection();
                    }
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "SSWS " + token);
                    conn.setRequestMethod("GET");

                    String ret = conn.getResponseMessage();
                    int retCode = conn.getResponseCode();
                    if (retCode == 200) {
                        tryAgain = false;
                        System.out.println(conn.getHeaderField("X-Rate-Limit-Remaining") + " "
                                + conn.getHeaderField("X-Rate-Limit-Limit") + " "
                                + conn.getHeaderField("X-Rate-Limit-Reset"));
                        rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        while ((line = rd.readLine()) != null) {
                            result += line;
                        }
                        rd.close();
                    } else if (retCode == 404) {
                        tryAgain = false;
                        rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        while ((line = rd.readLine()) != null) {
                            result += line;
                        }
                        rd.close();
                    } else {
                        tryAgain = true;
                        rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        while ((line = rd.readLine()) != null) {
                            result += line;
                        }
                        rd.close();
                        System.out.println(new Date() + " GET " + resource + " RETURNED " + retCode + ":" + ret);
                        System.out.println(new Date() + " ERRORSTREAM = " + result);
//                sendEmail("Rules Engine Error getting resource ", new Date() + " " + resource + " " + result);
                    }
                } catch (SocketTimeoutException e) {
                    tryAgain = true;
                    System.out.println(new Date() + " GET " + resource + " " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            result = e.getLocalizedMessage();
            System.out.println(new Date() + " GET " + resource + " " + e.getLocalizedMessage());
            e.printStackTrace();
//            sendEmail("Rules Engine Error", new Date() + " " + resource + " " + result);
        }
        return result;
    }

    public static String put(String resource, String token, String data) {
        String res = "";
        try {
            URL url = new URL(resource);
            HttpURLConnection conn;
            if (proxy != null) {
                conn = (HttpURLConnection) url.openConnection(proxy);
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }
            conn.setConnectTimeout(100000);
            conn.setReadTimeout(100000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "SSWS " + token);
            conn.setRequestProperty("Content-Type", "application/json");

            conn.setRequestMethod("PUT");
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.write(data.getBytes("UTF-8"));
            wr.flush();
            wr.close();
            String line;
            if (conn.getResponseCode() == 200 || conn.getResponseCode() == 201) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = rd.readLine()) != null) {
                    res += line;
                }
                rd.close();
                System.out.println(new Date() + " Application Update Success");
            } else if (conn.getResponseCode() == 204) {
                System.out.println(new Date() + " Application Update Success");
            } else {
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                while ((line = rd.readLine()) != null) {
                    res += line;
                }
                rd.close();
                System.out.println(new Date() + " PUT " + url.toString() + " OF " + data + " RETURNS " + conn.getResponseCode() + ":" + conn.getResponseMessage());
                System.out.println(new Date() + " ERRORSTREAM = " + res);
//                sendEmail("Rules Engine Error in PUT", new Date() + " " + resource + " " + data + " " + res);
            }
        } catch (Exception e) {
            res = e.getLocalizedMessage();
            System.out.println(new Date() + " PUT " + resource + " OF " + data);
            e.printStackTrace();
//            sendEmail("Rules Engine Error in PUT", new Date() + " " + resource + " " + data + " " + res);
        }
        return res;
    }

    public static String delete(String resource, String token) {
        String res = "";
        try {
            URL url = new URL(resource);
            HttpURLConnection conn;
            if (proxy != null) {
                conn = (HttpURLConnection) url.openConnection(proxy);
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "SSWS " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestMethod("DELETE");
            String line;
            if (conn.getResponseCode() == 200 || conn.getResponseCode() == 202) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = rd.readLine()) != null) {
                    res += line;
                }
                rd.close();
                System.out.println(new Date() + " DELETE " + url.toString());
                System.out.println(new Date() + " OUTSTREAM = " + res);
                System.out.println(new Date() + " Application Delete Success");
            } else if (conn.getResponseCode() == 204) {
                System.out.println(new Date() + " Application Delete Success");
            } else {
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                while ((line = rd.readLine()) != null) {
                    res += line;
                }
                rd.close();
                System.out.println(new Date() + " DELETE " + url.toString());
                System.out.println(new Date() + " ERRORSTREAM = " + res);
//                sendEmail("Rules Engine Error in DELETE", new Date() + " " + resource + " " + res);
            }
        } catch (Exception e) {
            res = e.getLocalizedMessage();
            System.out.println(new Date() + " DELETE " + resource + " LOCALIZEDMESSAGE " + res);
            e.printStackTrace();
//            sendEmail("Rules Engine Error in DELETE", new Date() + " " + resource + " " + res);
        }
        return res;
    }

    public static String post(String resource, String token, String data) {
        System.out.println(new Date() + " POST " + resource + data);
        String res = "";
        try {
            URL url = new URL(resource);
            HttpURLConnection conn;
            if (proxy != null) {
                conn = (HttpURLConnection) url.openConnection(proxy);
            } else {
                conn = (HttpURLConnection) url.openConnection();
            }
            conn.setConnectTimeout(100000);
            conn.setReadTimeout(100000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "SSWS " + token);
            conn.setRequestProperty("Content-Type", "application/json");

            conn.setRequestMethod("POST");
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.write(data.getBytes("UTF-8"));
            wr.flush();
            wr.close();
            String line;
            if (conn.getResponseCode() == 200 || conn.getResponseCode() == 201) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = rd.readLine()) != null) {
                    res += line;
                }
                rd.close();
            } else if (conn.getResponseCode() == 204) {
            } else {
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                while ((line = rd.readLine()) != null) {
                    res += line;
                }
                rd.close();
            }
        } catch (Exception e) {
            res = e.getLocalizedMessage();
            e.printStackTrace();
        }
        System.out.println(new Date() + " RESULT " + res);
        return res;
    }

    public static String getType(String urlPrefix, String appid, String attribute, String token) {
        String ret = "";
        String appsString = get(urlPrefix + "/apps/" + appid + "/user/schemas", token);
        JSONArray apps = new JSONArray(appsString);
        for (int i = 0; i < apps.length(); i++) {
            JSONObject obj = apps.getJSONObject(i).getJSONObject("schema").getJSONObject("properties");
            if (obj.has(attribute)) {
                ret = obj.getJSONObject(attribute).getString("type");
                break;
            }
        }
        return ret;
    }

    public static void updateAppUser(String app, HashMap<String, LinkedList<String>> attribute, String user, String urlPrefix, String token) {
        System.out.println(new Date() + " user id:" + user + " app:" + app + " attribute:" + attribute);
        String appsString = get(urlPrefix + "/apps?q=" + app, token);
        JSONArray apps = new JSONArray(appsString);
        for (int i = 0; i < apps.length(); i++) {
            if (apps.getJSONObject(i).getString("label").equals(app)) {
                String appid = apps.getJSONObject(i).getString("id");
                String appuser = get(urlPrefix + "/apps/" + appid + "/users/" + user, token);
                JSONObject appuserJSON = new JSONObject(appuser);
                JSONObject updatedappuser = new JSONObject();
                if (appuserJSON.has("errorCode")) {
                    updatedappuser.put("id", user);
                    updatedappuser.put("profile", new JSONObject());
                } else {
                    updatedappuser.put("profile", appuserJSON.getJSONObject("profile"));
                }
                for (Map.Entry<String, LinkedList<String>> entry : attribute.entrySet()) {
                    JSONArray array = new JSONArray();
                    for (String s : entry.getValue()) {
                        array.put(s);
                    }
                    String type = getType(urlPrefix, appid, entry.getKey(), token);
                    if (type.equals("string")) {
                        updatedappuser.getJSONObject("profile").put(entry.getKey(), array.getString(0));
                    } else if (type.equals("array")) {
                        updatedappuser.getJSONObject("profile").put(entry.getKey(), array);
                    } else {
                        // If we can't figure out the type, default to array.
                        updatedappuser.getJSONObject("profile").put(entry.getKey(), array);
                    }
                }
                String value = updatedappuser.toString();
                if (appuserJSON.has("errorCode")) {
                    String response = post(urlPrefix + "/apps/" + appid + "/users", token, value);
                } else {
                    String response = put(urlPrefix + "/apps/" + appid + "/users/" + user, token, value);
                }
            }
        }
    }

    public static void deleteAppUser(String app, String user, String urlPrefix, String token) {
        System.out.println(new Date() + " DELETE user id:" + user + " FROM app:" + app);
        String appsString = get(urlPrefix + "/apps?q=" + app, token);
        JSONArray apps = new JSONArray(appsString);
        for (int i = 0; i < apps.length(); i++) {
            if (apps.getJSONObject(i).getString("label").equals(app)) {
                String appid = apps.getJSONObject(i).getString("id");
                String appuser = get(urlPrefix + "/apps/" + appid + "/users/" + user, token);
                JSONObject appuserJSON = new JSONObject(appuser);
                if (appuserJSON.has("errorCode")) {
                    System.out.println(new Date() + " User not in app. No delete required.");
                } else {
                    String response = delete(urlPrefix + "/apps/" + appid + "/users/" + user, token);
                    System.out.println(new Date() + response);
                }
            }
        }
    }

    public static String[] groupsContainGroup(HashSet<String> groups, String group) {
        int count = 0;
        for (String g : groups) {
            if (group.endsWith("*") && g.startsWith(group.substring(0, group.length() - 1))) {
                count++;
            } else if (g.equals(group)) {
                count++;
            }
        }
        String found[] = new String[count];
        count = 0;
        for (String g : groups) {
            if (group.endsWith("*") && g.startsWith(group.substring(0, group.length() - 1))) {
                found[count++] = g;
            } else if (g.equals(group)) {
                found[count++] = g;
            }
        }
        return found;
    }

    public static void processUser(Rules rules, String user, String url, String token) throws MalformedURLException, IOException {
        HashSet<String> groups = getUserGroups(user, url, token);
        if (groups == null) {
            System.out.println(new Date() + " user id: " + user + " does not exist in this org!");
        } else {
            System.out.println(new Date() + " user id:" + user + " groups:" + groups);
            Rule[] rule = rules.rule;
            HashMap<String, HashMap<String, LinkedList<String>>> apps = new HashMap<String, HashMap<String, LinkedList<String>>>();
            for (int i = 0; i < rule.length; i++) {
                String matchedGroups[] = groupsContainGroup(groups, rule[i].group);
                for (String matchedGroup : matchedGroups) {
                    if (!apps.containsKey(rule[i].application)) {
                        apps.put(rule[i].application, new HashMap<String, LinkedList<String>>());
                    }
                    if (!apps.get(rule[i].application).containsKey(rule[i].attributeName)) {
                        apps.get(rule[i].application).put(rule[i].attributeName, new LinkedList<String>());
                    }
                    String value;
                    if (rule[i].group.endsWith("*")) {
                        value = rule[i].attributeValue.replace("*",
                                matchedGroup.substring(rule[i].group.length() - 1));
                    } else {
                        value = rule[i].attributeValue;
                    }
                    apps.get(rule[i].application).get(rule[i].attributeName).add(value);
                }
            }
            for (Map.Entry<String, HashMap<String, LinkedList<String>>> entry : apps.entrySet()) {
                updateAppUser(entry.getKey(), entry.getValue(), user, url, token);
            }
            HashMap<String, LinkedList<String>> appGroupMap = new HashMap<String, LinkedList<String>>();
            for (Rule r : rules.rule) {
                if (!appGroupMap.containsKey(r.application)) {
                    appGroupMap.put(r.application, new LinkedList<String>());
                }
                appGroupMap.get(r.application).add(r.group);
            }
            for (Map.Entry<String, LinkedList<String>> entry : appGroupMap.entrySet()) {
                boolean found = false;
                for (String gs : entry.getValue()) {
                    if (groupsContainGroup(groups, gs).length > 0) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    deleteAppUser(entry.getKey(), user, url, token);
                }
            }
        }
    }

    public static void saveToFile(String lasttime) {
        try {
            BufferedWriter b = new BufferedWriter(new FileWriter("lasttime.txt"));
            b.write(lasttime);
            b.close();
        } catch (Exception e) {
            e.printStackTrace();
//            sendEmail("Rules Engine Error", new Date() + " " + e.getLocalizedMessage());
        }
    }

    public static String readFromFile() {
        String ret = null;
        try {
            BufferedReader b = new BufferedReader(new FileReader("lasttime.txt"));
            ret = b.readLine();
        } catch (Exception e) {
            e.printStackTrace();
//            sendEmail("Rules Engine Error", new Date() + " " + e.getLocalizedMessage());
        }
        return ret;
    }

    /*
     public static void sendEmail(String subject, String text) {
     // Recipient's email ID needs to be mentioned.
     String to = "gmehta@amgen.com";

     // Sender's email ID needs to be mentioned
     String from = "svc-cldadagnt-prod@amgen.com";

     // Assuming you are sending email from localhost
     String host = "mailhost-i.amgen.com";

     // Get system properties
     Properties properties = System.getProperties();

     // Setup mail server
     properties.setProperty("mail.smtp.host", host);

     // Get the default Session object.
     Session session = Session.getDefaultInstance(properties);

     try {
     // Create a default MimeMessage object.
     MimeMessage message = new MimeMessage(session);

     // Set From: header field of the header.
     message.setFrom(new InternetAddress(from));

     // Set To: header field of the header.
     message.addRecipient(Message.RecipientType.TO,
     new InternetAddress(to));

     // Set Subject: header field
     message.setSubject(subject);

     // Now set the actual message
     message.setText(text);

     // Send message
     Transport.send(message);
     System.out.println("Sent message successfully....");
     } catch (MessagingException mex) {
     mex.printStackTrace();
     }
     }
     */
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println(new Date() + " Starting: " + args[0] + "," + args[1] + "," + args[2]
                + "," + args[3] + "," + args[4]);
        String urlPrefix = args[0];
        String token = args[1];
        int count = Integer.parseInt(args[2]);
        long period = Long.parseLong(args[3]);
        String eGroup = args[4];
        while (true) {
            try {
                System.out.println(new Date() + " Processing");
                JSONArray users = OktaGetAllUsers(urlPrefix, eGroup, token);
                for (int i = 0; i < users.length(); i++) {
                    JSONObject user = users.getJSONObject(i);
                    String status = user.getString("status");
                    String login = user.getJSONObject("profile").getString("login");
                    String id = user.getString("id");
                    if (!status.equals("LOCKED_OUT")) {
                        if (user.getJSONObject("profile").has("userType")) {
                            String userType = user.getJSONObject("profile").getString("userType");
                            if (userType.equals("External")) {
                                boolean lockit = false;
                                if (user.getJSONObject("profile").has("lastLocked")) {
                                    String lastLocked = user.getJSONObject("profile").getString("lastLocked");
                                    String tmp = lastLocked;
                                    DateFormat dateFormat;
                                    dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
                                    Date date = dateFormat.parse(lastLocked);
                                    tmp = dateFormat.format(date);
                                    Date today = new Date();
                                    long diff = today.getTime() - date.getTime();
                                    long diffSeconds = diff / 1000;
                                    long diffMinutes = diffSeconds / 60;
                                    long diffHours = diffMinutes / 60;
                                    long diffDays = diffHours / 24;
                                    if (diffDays > period) {
                                        for (int j = 0; j < count; j++) {
                                            String resp = post(urlPrefix + "/authn", token, "{\"username\":\""
                                                    + login + "\",\"password\":\"bogus\"}");
                                        }
                                        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
                                        Date now = new Date();
                                        String l = dateFormat.format(now);
                                        String resp = post(urlPrefix + "/users/" + id, token, "{\"profile\":{\"lastLocked\":\""
                                                + l + "\"}}");
                                    }
                                } else {
                                    DateFormat dateFormat;
                                    dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
                                    Date now = new Date();
                                    String l = dateFormat.format(now);
                                    String resp = post(urlPrefix + "/users/" + id, token, "{\"profile\":{\"lastLocked\":\""
                                            + l + "\"}}");
                                }
                            }
                        }
                    } else {
                        DateFormat dateFormat;
                        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
                        Date now = new Date();
                        String l = dateFormat.format(now);
                        post(urlPrefix + "/users/" + id, token, "{\"profile\":{\"lastLocked\":\""
                                + l + "\"}}");
                    }
                }
                try {
                    Thread.sleep(1000 * 60 * 60 * 24);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
