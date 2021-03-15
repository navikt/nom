package no.nav.nom.skjermede.personer.tps.config;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.jms.JmsConstants;
import com.ibm.msg.client.wmq.WMQConstants;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.skjermede.personer.tps.dto.SfePersonDataDTO;
import org.apache.logging.log4j.util.Strings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.connection.UserCredentialsConnectionFactoryAdapter;
import org.springframework.jms.support.converter.MarshallingMessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@EnableJms
@Configuration
public class JmsConfig {

    private static final int CCSID = 1208;

    @Bean
    public MessageConverter converter() {
        val marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan(SfePersonDataDTO.class.getPackage().getName());

        val converter = new MarshallingMessageConverter(marshaller);
        converter.setTargetType(MessageType.TEXT);

        return converter;
    }

    @Bean
    public ConnectionFactory jmsConnectionFactory(final JmsConfigProperties jmsProperties) throws URISyntaxException, JMSException {
        val uri = new URI(jmsProperties.getManagerUrl());

        val connectionFactory = new MQQueueConnectionFactory();
        connectionFactory.setHostName(uri.getHost());
        connectionFactory.setPort(uri.getPort());
        connectionFactory.setQueueManager(uri.getPath().replace("/", Strings.EMPTY));
        connectionFactory.setChannel(jmsProperties.getChannelName());
        connectionFactory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
        connectionFactory.setCCSID(CCSID);
        connectionFactory.setIntProperty(WMQConstants.JMS_IBM_CHARACTER_SET, CCSID);
        connectionFactory.setBooleanProperty(JmsConstants.USER_AUTHENTICATION_MQCSP, false);

        val adapter = new UserCredentialsConnectionFactoryAdapter();
        adapter.setTargetConnectionFactory(connectionFactory);
        adapter.setUsername(jmsProperties.getUsername());
        adapter.setPassword(Strings.EMPTY);

        return adapter;
    }
}
