package no.nav.nom.apps.api.clients.msgraph;

import java.util.List;

public record EmailMessage(
        Message message
) {
    public EmailMessage(String subject, String content, List<String> recipientsEmailAddresses) {
        this(new Message(subject, recipientsEmailAddresses, new Message.Body("Text", content), List.of(), List.of()));
    }

    public EmailMessage(String subject, String content, List<String> recipientsEmailAddresses, List<String> ccRecipientsEmailAddresses) {
        this(new Message(subject, recipientsEmailAddresses, new Message.Body("Text", content), ccRecipientsEmailAddresses, List.of()));
    }

    public EmailMessage(String subject, String content, List<String> recipientsEmailAddresses, List<String> ccRecipientsEmailAddresses, List<String> bccRecipientsEmailAddresses) {
        this(new Message(subject, recipientsEmailAddresses, new Message.Body("Text", content), ccRecipientsEmailAddresses, bccRecipientsEmailAddresses));
    }

    public record Message(
            String subject,
            Body body,
            List<Recipient> toRecipients,
            List<Recipient> ccRecipients,
            List<Recipient> bccRecipients
    ) {

        public Message(String subject, List<String> recipientsEmailAddresses, Body body, List<String> ccRecipientsEmailAddresses, List<String> bccRecipientsEmailAddresses) {
            this(
                subject,
                body,
                recipientsEmailAddresses.stream().map(Recipient::new).toList(),
                ccRecipientsEmailAddresses.stream().map(Recipient::new).toList(),
                bccRecipientsEmailAddresses.stream().map(Recipient::new).toList()
            );
        }

        public record Body(
                String contentType,
                String content
        ) {}

        public record Recipient(
                EmailAddress emailAddress
        ) {
            public Recipient(String address) {
                this(new EmailAddress(address));
            }

            public record EmailAddress(
                    String address
            ) {}
        }
    }
}