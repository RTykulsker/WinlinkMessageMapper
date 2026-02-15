/**

The MIT License (MIT)

Copyright (c) 2026, Robert Tykulsker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*/

package com.surftools.wimp.processors.std;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.utils.UtcDateTime;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.service.outboundMessage.AbstractBaseOutboundMessageEngine;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * when we are absolutely, positively finally done with an exercise ...
 *
 * transfer the published folder to where the web server can access
 *
 * transfer the entire exercise (input, output, published, winlink) to archive storage
 *
 * send email to relevant parties
 *
 * snapshot entire exercise directory to local disk
 */
public class FinalizeProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(FinalizeProcessor.class);

  static record EmailContext(String from, InternetAddress[] recipientAddresses, String password, String subject,
      String body, Session session) {
  };

  static record WinlinkContext(String subject, String sender, String source, String to, String body) {
  };

  private String emailPassword;
  private EmailContext emailContext;
  private WinlinkContext winlinkContext;
  private List<String> archiveRootNamesList = new ArrayList<>();
  private List<String> publicationRootNamesList = new ArrayList<>();

  private boolean doPublish = false;
  private boolean doArchive = false;
  private boolean doEmail = false;
  private boolean doEmailViaInternet = false;
  private boolean doEmailViaWinlink = false;
  private boolean doSnapshot = false;
  private boolean isFinalizing = false;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    var enabled = cm.getAsBoolean(Key.ENABLE_FINALIZE);
    if (!enabled) {
      logger.warn("#### Finalization not enabled.");
      return;
    }

    doPublish = initialize_publish();
    doArchive = initialize_archive();
    doEmail = initialize_email();
    doSnapshot = initialize_snapshot();

    isFinalizing = doPublish || doArchive || doEmail || doSnapshot;
    if (!isFinalizing) {
      logger.warn("#### Finalization not enabled because no sub-elements enabled.");
      return;
    }
  }

  @Override
  public void process() {
  }

  @Override
  public void postProcess() {
    if (!isFinalizing) {
      logger.warn("#### Finalization not enabled.");
      return;
    }

    do_publish();
    do_email(); // move up email, so that email_via_winlink file gets archived
    do_archive();
    do_snapshot();
  } // end postProcess

  private boolean initialize_publish() {
    var isEnabledViaConfig = cm.getAsBoolean(Key.ENABLE_FINALIZE_PUBLISHED);
    if (!isEnabledViaConfig) {
      logger.warn("### published finalizing not enabled via configuration");
      return false;
    }

    var publicationRootNames = cm.getAsString(Key.PATH_PUBLICATION);
    if (publicationRootNames == null) {
      logger.warn("### path.publication is null. Can't publish");
      return false;
    }
    var fields = publicationRootNames.split(",");

    for (var publicationRootName : fields) {
      var remoteFilePath = Path.of(publicationRootName, "REMOTE");
      var remoteFile = remoteFilePath.toFile();
      if (!remoteFile.exists()) {
        logger.warn("### remote file: " + remoteFilePath.toString() + " not found.");
        return false;
      } else {
        publicationRootNamesList.add(publicationRootName);
      }
    }

    var isOk = publicationRootNamesList.size() > 0;
    if (isOk) {
      logger.info("will publish to the following remotes: " + String.join(", ", publicationRootNamesList));
      return true;
    } else {
      logger.warn("### NO remote file system not found. Can't publish");
      return false;
    }
  }

  private boolean initialize_archive() {
    var isEnabledViaConfig = cm.getAsBoolean(Key.ENABLE_FINALIZE_ARCHIVE);
    if (!isEnabledViaConfig) {
      logger.warn("### archive finalizing not enabled via configuration");
      return false;
    }

    var archiveRootNames = cm.getAsString(Key.PATH_ARCHIVE);
    if (archiveRootNames == null) {
      logger.warn("### path.archive is null. Can't publish");
      return false;
    }
    var fields = archiveRootNames.split(",");

    for (var archiveRootName : fields) {
      var remoteFilePath = Path.of(archiveRootName, "REMOTE");
      var remoteFile = remoteFilePath.toFile();
      if (!remoteFile.exists()) {
        logger.warn("### remote file: " + remoteFilePath.toString() + " not found.");
        return false;
      } else {
        archiveRootNamesList.add(archiveRootName);
      }
    }

    var isOk = archiveRootNamesList.size() > 0;
    if (isOk) {
      logger.info("will archive to the following remotes: " + String.join(", ", archiveRootNamesList));
      return true;
    } else {
      logger.warn("### NO remote file system not found. Can't archive");
      return false;
    }

  }

  private boolean initialize_email() {
    doEmailViaInternet = initialize_email_via_internet();
    doEmailViaWinlink = initialize_email_via_winlink();
    return doEmailViaInternet || doEmailViaWinlink;
  }

  private boolean initialize_email_via_internet() {
    var isEnabledViaConfig = cm.getAsBoolean(Key.ENABLE_FINALIZE_EMAIL_INTERNET);
    if (!isEnabledViaConfig) {
      logger.warn("### email via Internet finalizing not enabled via configuration");
      return false;
    }

    var from = cm.getAsString(Key.EMAIL_NOTIFICATION_FROM);
    if (from == null || from.isBlank()) {
      logger.info("NO email notifications, because from is null");
      return false;
    }

    var toListString = cm.getAsString(Key.EMAIL_NOTIFICATION_TO);
    if (toListString == null || toListString.isBlank()) {
      logger.info("NO email notifications, because to is null");
      return false;
    }

    var fields = toListString.split(",");
    var recipientAddresses = new InternetAddress[fields.length];
    for (var i = 0; i < fields.length; ++i) {
      try {
        recipientAddresses[i] = new InternetAddress(fields[i]);
      } catch (Exception e) {
        logger.warn("Exception parsing adddress[" + i + "] (" + fields[i] + "): " + e.getMessage());
        return false;
      }
    }

    var passwordFileName = cm.getAsString(Key.EMAIL_NOTIFICATION_PASSWORD_FILEPATH);
    if (passwordFileName == null || passwordFileName.isBlank()) {
      logger.info("NO email notifications, because password.filePath is null");
      return false;
    }

    var passwordFile = new File(passwordFileName);
    if (!passwordFile.exists()) {
      logger.info("NO email notifications, because password file: " + passwordFileName + " not found");
      return false;
    }

    try {
      emailPassword = Files.readString(Path.of(passwordFileName));
    } catch (IOException e) {
      logger.warn("Exception reading password filePath:" + passwordFileName + ", " + e.getMessage());
      return false;
    }

    var subject = cm.getAsString(Key.EMAIL_NOTIFICATION_SUBJECT, "ETO: WLT processings is complete for #DATE#");
    subject = subject.replaceAll("#DATE#", dateString);

    var body = cm.getAsString(Key.EMAIL_NOTIFICATION_BODY, "Ready for you to send groups.io message to all");
    body = body.replaceAll("#DATE#", dateString);

    Properties props = new Properties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.host", "smtp.gmail.com");
    props.put("mail.smtp.port", "587");

    Session session = null;
    try {
      session = Session.getInstance(props, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(from, emailPassword);
        }
      });

    } catch (Exception e) {
      logger.warn("Exception creating session: " + e.getLocalizedMessage());
      session = null;
      return false;
    }

    emailContext = new EmailContext(from, recipientAddresses, passwordFileName, subject, body, session);

    logger.info("Email notification from: " + emailContext.from());
    logger.info("Email notification to: " + toListString);
    logger.info("Email notification subject: " + emailContext.subject());
    logger.info("Email notification body: " + emailContext.body());

    return true;
  }

  private boolean initialize_email_via_winlink() {
    var isEnabledViaConfig = cm.getAsBoolean(Key.ENABLE_FINALIZE_EMAIL_WINLINK);
    if (!isEnabledViaConfig) {
      logger.warn("### email via Winlink finalizing not enabled via configuration");
      return false;
    }

    var toListString = cm.getAsString(Key.EMAIL_NOTIFICATION_TO);
    if (toListString == null || toListString.isBlank()) {
      logger.info("NO winlink notifications, because to is null");
      return false;
    }

    var source = cm.getAsString(Key.WINLINK_NOTIFICATION_SOURCE, "ETO-FEEDBACK");
    var sender = cm.getAsString(Key.WINLINK_NOTIFICATION_SENDER, "ETO-FEEDBACK");

    var subject = cm.getAsString(Key.EMAIL_NOTIFICATION_SUBJECT, "ETO: WLT processings is complete for #DATE#");
    subject = subject.replaceAll("#DATE#", dateString);

    var body = cm.getAsString(Key.EMAIL_NOTIFICATION_BODY, "Ready for you to send groups.io message to all");
    body = body.replaceAll("#DATE#", dateString);

    winlinkContext = new WinlinkContext(subject, sender, source, toListString, body);

    logger.info("Winlink notification source: " + winlinkContext.source());
    logger.info("Winlink notification sender: " + winlinkContext.sender());
    logger.info("Winlink notification to: " + winlinkContext.to());
    logger.info("Winlink notification subject: " + winlinkContext.subject());
    logger.info("Winlink notification body: " + winlinkContext.body());

    return true;
  }

  private boolean initialize_snapshot() {
    var isEnabledViaConfig = cm.getAsBoolean(Key.ENABLE_FINALIZE_SNAPSHOT);
    if (!isEnabledViaConfig) {
      logger.warn("### snapshot finalizing not enabled via configuration");
      return false;
    }

    return true;
  }

  private void do_publish() {
    if (!doPublish) {
      logger.info("publish finalization not enabled.");
      return;
    }

    var yearString = dateString.substring(0, 4);
    for (var publicationRootName : publicationRootNamesList) {
      var remotePath = Path.of(publicationRootName, "results", yearString, dateString);
      logger
          .info("begin copy of published local: " + publishedPath.toString() + " to remote: " + remotePath.toString());
      FileUtils.makeDirIfNeeded(remotePath);
      FileUtils.copyDirectory(publishedPath, remotePath);
      logger.info("end copy of published local: " + publishedPath.toString() + " to remote: " + remotePath.toString());
    }
  }

  /**
   * copy ALL exercise files to remote .../results/yyyy/yyyy-mm-dd/ folder
   */
  private void do_archive() {
    if (!doArchive) {
      logger.info("archive finalization not enabled.");
      return;
    }

    // var yearString = dateString.substring(0, 4);
    for (var archiveRootName : archiveRootNamesList) {
      var localPath = exercisePath;
      // NOTE WELL: no yearString here, it is already in paths.archive, etc.
      var remotePath = Path.of(archiveRootName, "results", dateString);
      logger.info("begin archive of local exercise: " + localPath.toString() + " to remote: " + remotePath.toString());
      FileUtils.makeDirIfNeeded(remotePath);
      FileUtils.copyDirectory(localPath, remotePath);
      logger.info("end archive of local exercise: " + localPath.toString() + " to remote: " + remotePath.toString());
    }
  }

  /**
   * send email to various ETO members to notify that weekly practice processing is complete
   */
  private void do_email() {
    if (!doEmail) {
      logger.info("email finalization not enabled.");
      return;
    }

    if (doEmailViaInternet && emailContext != null) {
      try {
        var emailMessage = new MimeMessage(emailContext.session);
        emailMessage.setFrom(new InternetAddress(emailContext.from));
        emailMessage.setRecipients(Message.RecipientType.TO, emailContext.recipientAddresses);
        emailMessage.setSubject(emailContext.subject);
        emailMessage.setText(emailContext.body);

        Transport.send(emailMessage);

        logger.info("email sent via Internet");
      } catch (Exception e) {
        logger.error("### Exception sending email notification message: " + e.getMessage());
      }
    }

    if (doEmailViaWinlink && winlinkContext != null) {
      var now = LocalDateTime.now();
      var body = winlinkContext.body();
      body = body.replaceAll("<", "&lt;");
      body = body.replaceAll("<=", "&lt;=3D");
      body = body.replaceAll(">", "&gt;");
      body = body.replaceAll(">=", "&gt;=3D");

      var toArray = winlinkContext.to.split(",");
      var toList = Arrays.asList(toArray);
      var toString = String.join(",\n ", toList);
      var text = WINLINK_TEMPLATE;
      text = text.replace("#MESSAGE_ID#", AbstractBaseOutboundMessageEngine.generateMid("finalizer"));
      text = text.replaceAll("#SUBJECT#", winlinkContext.subject());
      text = text.replaceAll("#SENDER#", winlinkContext.sender());
      text = text.replaceAll("#SOURCE#", winlinkContext.source());
      text = text.replaceAll("#TO#", toString);
      text = text.replaceAll("#BODY#", body);
      text = text.replaceAll("#MESSAGE_TIME#", DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").format(now));
      text = text
          .replaceAll("#MIME_TIME#",
              DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss +0000").format(UtcDateTime.ofNow()));
      try {
        var path = Path.of(winlinkPathName, "FINAL_NOTIFICATIONS.xml");
        Files.writeString(path, text);
        logger.info("wrote Winlink notification file to " + path.toString());
      } catch (Exception e) {
        logger.error("### Exception writing winlink notification message: " + e.getMessage());
      }
    }
  }

  private void do_snapshot() {
    if (!doSnapshot) {
      logger.info("snapshot finalization not enabled.");
      return;
    }

    var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
    LocalDateTime now = LocalDateTime.now();
    var timestamp = now.format(formatter);

    var tempPath = Path.of(exercisesPathName, "FINAL-" + timestamp);
    FileUtils.copyDirectory(exercisePath, tempPath);
    var tempDir = tempPath.toFile();
    var finalDir = Path.of(exercisePathName, "FINAL-" + timestamp).toFile();
    tempDir.renameTo(finalDir);
    logger.info("snapshot copied exercise dir to: " + finalDir.toString());
  }

  public static final String WINLINK_TEMPLATE = """
      <?xml version="1.0"?>
      <Winlink_Express_message_export>
        <export_parameters>
        </export_parameters>
        <message_list>
          <message>
            <id>#MESSAGE_ID#</id>
            <foldertype>Fixed</foldertype>
            <folder>Outbox</folder>
            <subject>#SUBJECT#</subject>
            <time>#MESSAGE_TIME#</time>
            <sender>ETO-FEEDBACK</sender>
            <mime>Date: #MIME_TIME#
      From: #SENDER#@winlink.org
      Reply-To: #SENDER#@winlink.org
      Subject: #SUBJECT#
      To: #TO#
      Message-ID: #MESSAGE_ID#
      X-Source: #SOURCE#
      MIME-Version: 1.0
      Content-Type: multipart/mixed; boundary="boundarybEDjmg=="

      --boundarybEDjmg==
      Content-Type: text/plain; charset="iso-8859-1"
      Content-Transfer-Encoding: quoted-printable

      #BODY#
      --boundarybEDjmg==--</mime>
          </message>
        </message_list>
      </Winlink_Express_message_export>

            """;

}
