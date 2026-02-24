package io.mosip.digitalcard.service;

import java.util.Map;

public interface WhatsAppHelperService {
    void sendDigitalCardInWhatsApp(
            String fileName,
            Map<String, Object> attributes,
            byte[] pdfBytes,
            String templateLang
    );
}
