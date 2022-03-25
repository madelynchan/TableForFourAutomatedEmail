/*
Automated Gmail Editor and Sender, by Madelyn Chan
Used for TableForfour for AAIV of UW
 */

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/*

 */
public class GmailAPI {
    private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String AAIV_EMAIL = "aaivtablefor4@gmail.com";
    private static final String T44_EMAIL_SUBJECT = "AAIV Table For Four - Fall 2021";
    private static final boolean send = false;

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = GmailAPI.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

//    /**
//     * Create a MimeMessage using the parameters provided.
//     *
//     * @param to email address of the receiver
//     * @param from email address of the sender, the mailbox account
//     * @param subject subject of the email
//     * @param bodyText body text of the email
//     * @return the MimeMessage to be used to send email
//     * @throws MessagingException
//     */
    public static MimeMessage createEmail(String to, String name, String restaurant) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(AAIV_EMAIL));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(T44_EMAIL_SUBJECT);
        email.setText(makeMessageBody(name, restaurant));
        return email;
    }

    /**
     * Send an email from the user's mailbox to its recipient.
     *
     * @param service Authorized Gmail API instance.
     * @param userId User's email address. The special value "me"
     * can be used to indicate the authenticated user.
     * @param emailContent Email to be sent.
     * @return The sent message
     * @throws MessagingException
     * @throws IOException
     */
    public static Message sendMessage(Gmail service,
                                      String userId,
                                      MimeMessage emailContent)
            throws MessagingException, IOException {
        Message message = createMessageWithEmail(emailContent);
        message = service.users().messages().send(userId, message).execute();

        System.out.println(message);
        return message;
    }

    /**
     * Create a message from an email.
     *
     * @param emailContent Email to be set to raw of message
     * @return a message containing a base64url encoded email
     * @throws IOException
     * @throws MessagingException
     */
    public static Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    /**
     *
     * @param name
     * @param restaurant
     * @return
     */
    private static String makeMessageBody(String name, String restaurant) {
        String firstName = name.split(" ")[0];
        StringBuffer sb = new StringBuffer();
        sb.append("Whale hi there " + firstName +  "!\n" + "\n" +  "Thank you for signing up for AAIV's first Table For Four of 2021. Your assigned restaurant is " + restaurant + ". Please be there tonight (11/18) on time at 5:30pm or even a little early.\n" +
                "\n" +
                "Remember, your other group members are a surprise until you get to the restaurant, so be on the lookout!\n" +
                "\n" +
                "If you arrive first, wait outside for the rest of your group to show up, or if it is already close to 5:45 and your group is only missing one person, go ahead and sit down, as some people may be arriving late.\n" +
                "\n" +
                "If you are unable to find your group, text Maddie Chan at (206) 747-9647 and she will try to help locate your missing member(s). \n" +
                "\n" +
                "Walk down to the Hub, room 334 together at 7:00pm for large group after dinner. \n" + "\n" + "We hope you have fun with your new pals and that you are as excited to bring back this AAIV tradition as we are.");
        return sb.toString();
    }

    // returns map in the form: Key: 1 -> Value: "mee sum, 4343 university way NE"
    public static HashMap<Integer, String> parseRestaurants (String filename) {
        // You'll need to:
        //  - Split each line into its individual parts
        //  - Collect the data into some convenient data structure(s) to return to the graph building code
        List<String> lines = readLines(filename);
        HashMap<Integer, String> data = new HashMap<>();
        for (String line : lines) {
            String name = line.split(",")[0];
            int groupNum = Integer.valueOf(line.split(",")[1]);
            StringBuffer sb = new StringBuffer();
            data.put(groupNum, name);
        }
        return data;
    }

    // returns map in the form: Key:1->Value:["maddie chan,mgchan@gmail.com","toby wong,twong@uw.edu"]
    public static HashMap<Integer, HashSet<String>> parseStudents(String filename) {
        // You'll need to:
        //  - Split each line into its individual parts
        //  - Collect the data into some convenient data structure(s) to return to the graph building code
        List<String> lines = readLines(filename);
        HashMap<Integer, HashSet<String>> data = new HashMap<>();
        for (String line : lines) {
            String name = line.split(",")[0];
            String email = line.split(",")[1];
            int groupNum = Integer.valueOf(line.split(",")[2]);
            StringBuffer sb = new StringBuffer();
            sb.append(name);
            sb.append(",");
            sb.append(email);
            if (!data.containsKey(groupNum)) {
                data.put(groupNum, new HashSet<String>());
            }
            data.get(groupNum).add(sb.toString());
        }
        return data;
    }

    /**
     * Reads all lines contained within the provided data file, which is located
     * relative to the data/ folder in this parser's classpath.
     *
     * @param filename The file to read.
     * @throws IllegalArgumentException if the file doesn't exist, has an invalid name, or can't be read
     * @return A new {@link List<String>} containing all lines in the file.
     */
    private static List<String> readLines(String filename) {
        InputStream stream = GmailAPI.class.getResourceAsStream(filename);
        if (stream == null) {
            throw new IllegalArgumentException("No such file: " + filename);
        }
        return new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.toList());
    }


    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Print the labels in the user's account.
        String user = "me";
        ListLabelsResponse listResponse = service.users().labels().list(user).execute();
        List<Label> labels = listResponse.getLabels();
        if (labels.isEmpty()) {
            System.out.println("No labels found.");
        } else {
            System.out.println("Labels:");
            for (Label label : labels) {
                System.out.printf("- %s\n", label.getName());
            }
        }
        /*
        IMPORTANT - restaurants.csv must be in the form "restaurant 1,group_num<\n>restaurant 2,group_num" (restaurant names can have spaces, but
                    there can be no spaces next to the comma or after the group number)
                    students.csv must be in the form "student name,email@email.com,group_num" (student names can also have spaces, but there must
                    not be spaces next to either of the commas, and the number of values in one line must be 3
         */
        Map<Integer, String> restaurants = parseRestaurants("restaurants.csv");
        Map<Integer, HashSet<String>> students = parseStudents("students copy.csv");

        try {
            /*
            for every key (ie groupNum) in students (or restraunts they are the same), pull the list of 4 students
            from students and then the restaurant name
                then for every student in group name, send the messages (we should really test this before sending though lol)
            */
            for (int group : students.keySet()) {
                HashSet<String> currGroup = students.get(group);
                String restaurant = restaurants.get(group);
                String restName = restaurant.split(",")[0];
                for (String student : currGroup) {
                    String name = student.split(",")[0];
                    String email = student.split(",")[1];
                    MimeMessage emailToStudent = createEmail(email, name, restName);
                    System.out.println(emailToStudent.getContent());

                    // THIS SENDS THE ACTUAL MESSAGE! IF YOU ARE JUST TESTING, set boolean send = false
                    if (send) {
                        sendMessage(service, user, emailToStudent);
                    }
                }
            }
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}