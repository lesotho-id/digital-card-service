package io.mosip.digitalcard.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.digitalcard.constant.ApiName;
import io.mosip.digitalcard.dto.ErrorDTO;
import io.mosip.digitalcard.dto.NotificationResponseDTO;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class NotificationUtil {

    private Logger log = DigitalCardRepoLogger.getLogger(NotificationUtil.class);

    @Autowired
    private RestClient restApiClient;

    @Autowired
    private TemplateGenerator templateGenerator;

    @Autowired
    private ObjectMapper mapper;

    @Value("${mosip.digitalcard.default.email.sub:Digital Card Attached!}")
    private String defaultEmailSub;

    @Value("${mosip.digitalcard.default.email:Your digital identity Card is attached}")
    private String defaultEmail;

    public List<NotificationResponseDTO> emailNotification(List<String> emailIds, String fileName, String emailContentTpl, String emailSubTpl, Map<String, Object> attributes,
                                                           byte[] attachmentFile, String templateLang) throws Exception {
        log.info("sessionId", "idType", "id", "In emailNotification method of NotificationUtil service");
        HttpEntity<byte[]> doc = null;
        String fileText = null;
        MultiValueMap<Object, Object> emailMap = new LinkedMultiValueMap<>();
        if (attachmentFile != null) {
            LinkedMultiValueMap<String, String> pdfHeaderMap = new LinkedMultiValueMap<>();
            pdfHeaderMap.add("Content-disposition",
                    "form-data; name=attachments; filename=" + fileName + ".pdf");
            pdfHeaderMap.add("Content-type", "application/pdf");
            doc = new HttpEntity<>(attachmentFile, pdfHeaderMap);
            emailMap.add("attachments", doc);
        }
        List<NotificationResponseDTO> notifierResponseList = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        emailMap.add("mailContent", getEmailContent(emailContentTpl, attributes, templateLang));
        emailMap.add("mailSubject", getEmailSubject(emailSubTpl, attributes, templateLang));

        log.info("sessionId", "idType", "id",
                "In emailNotification method of NotificationUtil service emailResourceUrl: " + ApiName.KERNEL_NOTIFICATION_URL);
        emailIds.forEach(emailId -> {
            try {
                notifierResponseList.add(sendEmail(emailId, headers, emailMap));
            } catch (Exception e) {
                log.error("Failed to send notification via email.{}", emailId, e);
            }
        });
        return notifierResponseList;
    }

    private NotificationResponseDTO sendEmail( String emailId, HttpHeaders headers, MultiValueMap<Object, Object> emailMap) throws Exception {
        NotificationResponseDTO notifierResponse = new NotificationResponseDTO();
        try {
            emailMap.set("mailTo", emailId);
            HttpEntity<MultiValueMap<Object, Object>> httpEntity = new HttpEntity<>(emailMap, headers);
            ResponseWrapper<?> responseWrapper = (ResponseWrapper<?>) restApiClient.postApi(ApiName.KERNEL_NOTIFICATION_URL, null, "", "",
                    MediaType.MULTIPART_FORM_DATA, httpEntity, ResponseWrapper.class);
            if (responseWrapper != null) {
                notifierResponse = mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()), NotificationResponseDTO.class);
                if (notifierResponse != null) {
                    if (notifierResponse.getErrors() != null && !notifierResponse.getErrors().isEmpty()) {
                        ErrorDTO error = (ErrorDTO) notifierResponse.getErrors().get(0);
                        log.error("Received failure response from notification service: ", error.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error while sending pdf email.", ExceptionUtils.getStackTrace(e));
            throw e;
        }
        return notifierResponse;
    }

    private String getEmailContent(String emailContentTpl, Map<String, Object> attributes, String preferredLang) throws Exception {

        InputStream in = templateGenerator.getTemplate(emailContentTpl, attributes, preferredLang);
        if (in == null) {
            return defaultEmail;
        }
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String getEmailSubject(String emailSubTpl, Map<String, Object> attributes, String templateLang) throws Exception {

        InputStream in = templateGenerator.getTemplate(emailSubTpl, attributes, templateLang);
        if (in == null) {
            return defaultEmailSub;
        }
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    public NotificationResponseDTO whatsAppNotification(String whatsappNumber, String fileName, String templateCode, Map<String, Object> attributes, byte[] attachmentFile, String templateLang) throws Exception {

        log.info("Sending WhatsApp notification");

        MultiValueMap<Object, Object> map = new LinkedMultiValueMap<>();
        if (attachmentFile != null) {
            LinkedMultiValueMap<String, String> fileHeaders = new LinkedMultiValueMap<>();
            fileHeaders.add("Content-Disposition",
                    "form-data; name=files; filename=" + fileName + ".pdf");
            fileHeaders.add("Content-Type", "application/pdf");

            HttpEntity<byte[]> fileEntity = new HttpEntity<>(attachmentFile, fileHeaders);

            map.add("files", fileEntity);
        }
        map.add("recipient", whatsappNumber);
        map.add("message", getEmailContent(templateCode, attributes, templateLang));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<Object, Object>> httpEntity =
                new HttpEntity<>(map, headers);

        ResponseWrapper<?> responseWrapper = (ResponseWrapper<?>) restApiClient.postApi(ApiName.WHATSAPPNOTIFIER, null, "", "", MediaType.MULTIPART_FORM_DATA, httpEntity, ResponseWrapper.class);

        return mapper.readValue(mapper.writeValueAsString(responseWrapper.getResponse()), NotificationResponseDTO.class
        );
    }
}