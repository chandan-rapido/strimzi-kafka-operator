/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.kafkaclients.externalClients;

import io.strimzi.systemtest.Constants;
import io.strimzi.systemtest.kafkaclients.AbstractKafkaClient;
import io.strimzi.systemtest.kafkaclients.KafkaClientOperations;
import io.strimzi.systemtest.kafkaclients.KafkaClientProperties;
import io.strimzi.systemtest.resources.crd.KafkaResource;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntPredicate;

/**
 * The BasicExternalKafkaClient for sending and receiving messages with basic properties. The client is using an external listeners.
 */
public class BasicExternalKafkaClient extends AbstractKafkaClient implements KafkaClientOperations {

    private static final Logger LOGGER = LogManager.getLogger(BasicExternalKafkaClient.class);

    public static class Builder extends AbstractKafkaClient.Builder<Builder> {

        @Override
        public BasicExternalKafkaClient build() {
            return new BasicExternalKafkaClient(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

    private BasicExternalKafkaClient(Builder builder) {
        super(builder);
    }

    public int sendMessagesPlain() {
        return sendMessagesPlain(Constants.GLOBAL_CLIENTS_TIMEOUT);
    }

    /**
     * Send messages to external entrypoint of the cluster with PLAINTEXT security protocol setting
     * @return future with sent message count
     */
    public int sendMessagesPlain(long timeoutMs) {

        String clientName = "sender-plain-" + this.clusterName;
        CompletableFuture<Integer> resultPromise = new CompletableFuture<>();
        IntPredicate msgCntPredicate = x -> x == messageCount;

        KafkaClientProperties properties = new KafkaClientProperties.KafkaClientPropertiesBuilder()
            .withNamespaceName(namespaceName)
            .withClusterName(clusterName)
            .withBootstrapServerConfig(getExternalBootstrapConnect(namespaceName, clusterName))
            .withKeySerializerConfig(StringSerializer.class)
            .withValueSerializerConfig(StringSerializer.class)
            .withClientIdConfig(kafkaUsername + "-producer")
            .withSecurityProtocol(SecurityProtocol.PLAINTEXT)
            .withSharedProperties()
            .build();

        try (Producer plainProducer = new Producer(properties, resultPromise, msgCntPredicate, this.topicName, clientName)) {

            plainProducer.getVertx().deployVerticle(plainProducer);

            return plainProducer.getResultPromise().get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public int sendMessagesTls() {
        return sendMessagesTls(Constants.GLOBAL_CLIENTS_TIMEOUT);
    }

    /**
     * Send messages to external entrypoint of the cluster with SSL security protocol setting
     * @return future with sent message count
     */
    public int sendMessagesTls(long timeoutMs) {

        String clientName = "sender-ssl" + this.clusterName;
        CompletableFuture<Integer> resultPromise = new CompletableFuture<>();
        IntPredicate msgCntPredicate = x -> x == messageCount;

        String caCertName = this.caCertName == null ?
                KafkaResource.getKafkaExternalListenerCaCertName(this.namespaceName, clusterName) : this.caCertName;
        LOGGER.info("Going to use the following CA certificate: {}", caCertName);

        KafkaClientProperties properties = new KafkaClientProperties.KafkaClientPropertiesBuilder()
            .withNamespaceName(namespaceName)
            .withClusterName(clusterName)
            .withBootstrapServerConfig(getExternalBootstrapConnect(namespaceName, clusterName))
            .withKeySerializerConfig(StringSerializer.class)
            .withValueSerializerConfig(StringSerializer.class)
            .withClientIdConfig(kafkaUsername + "-producer")
            .withCaSecretName(caCertName)
            .withKafkaUsername(kafkaUsername)
            .withSecurityProtocol(securityProtocol)
            .withSaslMechanism("")
            .withSharedProperties()
            .build();

        try (Producer tlsProducer = new Producer(properties, resultPromise, msgCntPredicate, this.topicName, clientName)) {

            tlsProducer.getVertx().deployVerticle(tlsProducer);

            return tlsProducer.getResultPromise().get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public int receiveMessagesPlain() {
        return receiveMessagesPlain(Constants.GLOBAL_CLIENTS_TIMEOUT);
    }

    /**
     * Receive messages to external entrypoint of the cluster with PLAINTEXT security protocol setting
     * @return
     */
    public int receiveMessagesPlain(long timeoutMs) {

        String clientName = "receiver-plain-" + clusterName;
        CompletableFuture<Integer> resultPromise = new CompletableFuture<>();
        IntPredicate msgCntPredicate = x -> x == messageCount;

        KafkaClientProperties properties = new KafkaClientProperties.KafkaClientPropertiesBuilder()
            .withNamespaceName(namespaceName)
            .withClusterName(clusterName)
            .withBootstrapServerConfig(getExternalBootstrapConnect(namespaceName, clusterName))
            .withKeyDeserializerConfig(StringDeserializer.class)
            .withValueDeserializerConfig(StringDeserializer.class)
            .withClientIdConfig(kafkaUsername + "-consumer")
            .withAutoOffsetResetConfig(OffsetResetStrategy.EARLIEST)
            .withGroupIdConfig(consumerGroup)
            .withSecurityProtocol(SecurityProtocol.PLAINTEXT)
            .withSharedProperties()
            .build();

        try (Consumer plainConsumer = new Consumer(properties, resultPromise, msgCntPredicate, this.topicName, clientName)) {

            plainConsumer.getVertx().deployVerticle(plainConsumer);

            return plainConsumer.getResultPromise().get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public int receiveMessagesTls() {
        return receiveMessagesTls(Constants.GLOBAL_CLIENTS_TIMEOUT);
    }

    /**
     * Receive messages to external entrypoint of the cluster with SSL security protocol setting
     * @return future with received message count
     */
    public int receiveMessagesTls(long timeoutMs) {

        String clientName = "receiver-ssl-" + clusterName;
        CompletableFuture<Integer> resultPromise = new CompletableFuture<>();
        IntPredicate msgCntPredicate = x -> x == messageCount;

        String caCertName = this.caCertName == null ?
                KafkaResource.getKafkaExternalListenerCaCertName(this.namespaceName, this.clusterName) : this.caCertName;
        LOGGER.info("Going to use the following CA certificate: {}", caCertName);

        KafkaClientProperties properties = new KafkaClientProperties.KafkaClientPropertiesBuilder()
            .withNamespaceName(namespaceName)
            .withClusterName(clusterName)
            .withBootstrapServerConfig(getExternalBootstrapConnect(namespaceName, clusterName))
            .withKeyDeserializerConfig(StringDeserializer.class)
            .withValueDeserializerConfig(StringDeserializer.class)
            .withClientIdConfig(kafkaUsername + "-consumer")
            .withAutoOffsetResetConfig(OffsetResetStrategy.EARLIEST)
            .withGroupIdConfig(consumerGroup)
            .withSecurityProtocol(securityProtocol)
            .withCaSecretName(caCertName)
            .withKafkaUsername(kafkaUsername)
            .withSaslMechanism("")
            .withSharedProperties()
            .build();

        try (Consumer tlsConsumer = new Consumer(properties, resultPromise, msgCntPredicate, this.topicName, clientName)) {

            tlsConsumer.getVertx().deployVerticle(tlsConsumer);

            return tlsConsumer.getResultPromise().get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "BasicKafkaClient{" +
                "topicName='" + topicName + '\'' +
                ", namespaceName='" + namespaceName + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", messageCount=" + messageCount +
                ", consumerGroup='" + consumerGroup + '\'' +
                ", kafkaUsername='" + kafkaUsername + '\'' +
                ", securityProtocol='" + securityProtocol + '\'' +
                ", caCertName='" + caCertName + '\'' +
                '}';
    }
}
