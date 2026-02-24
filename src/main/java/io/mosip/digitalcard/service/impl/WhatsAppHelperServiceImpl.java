package io.mosip.digitalcard.service.impl;

import io.mosip.digitalcard.constant.DigitalCardConstants;
import io.mosip.digitalcard.service.WhatsAppHelperService;
import io.mosip.digitalcard.util.DigitalCardRepoLogger;
import io.mosip.digitalcard.util.NotificationUtil;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@ConditionalOnExpression("${mosip.digitalcard.whatsapp.enable.flag:false}")
public class WhatsAppHelperServiceImpl implements WhatsAppHelperService {

    private static final String UIN_CARD_TEMPLATE = "RPR_UIN_CARD_EMAIL";
    private static final String VID_CARD_TEMPLATE = "RPR_VID_CARD_EMAIL";

    private Logger logger = DigitalCardRepoLogger.getLogger(WhatsAppHelperServiceImpl.class);

    @Autowired
    private NotificationUtil notificationUtil;

    @Override
    public void sendDigitalCardInWhatsApp(String fileName, Map<String, Object> attributes,byte[] pdfBytes,String templateLang) {

        String whatsappNumber = (String) attributes.get("whatsappNumber");
        if (whatsappNumber == null || whatsappNumber.isBlank()) {
            return;
        }

        try {
            notificationUtil.whatsAppNotification(
                    whatsappNumber,
                    fileName,
                    (attributes.containsKey(DigitalCardConstants.VID_CARD)
                            ? VID_CARD_TEMPLATE
                            : UIN_CARD_TEMPLATE),
                    attributes,
                    pdfBytes,
                    templateLang
            );

            logger.info("UIN sent successfully via WhatsApp to {}", whatsappNumber);

        } catch (Exception e) {
            logger.error("Failed to send digital card via WhatsApp to {}", whatsappNumber, e);
        }
    }
}