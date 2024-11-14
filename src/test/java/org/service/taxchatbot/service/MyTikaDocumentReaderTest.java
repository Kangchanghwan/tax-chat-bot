package org.service.taxchatbot.service;

import groovy.util.logging.Slf4j;
import org.junit.jupiter.api.Test;
import org.service.taxchatbot.service.etl.MyTikaDocumentReader;
import org.service.taxchatbot.service.etl.MyTokenTextSplitter;
import org.service.taxchatbot.service.rag.ReReadingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@SpringBootTest
@Slf4j
class MyTikaDocumentReaderTest {

    @Autowired
    MyTikaDocumentReader myTikaDocumentReader;
    @Autowired
    MyTokenTextSplitter myTokenTextSplitter;
    @Autowired
    VectorStore vectorStore;
    @Autowired
    OpenAiChatModel chatModel;
    @Autowired
    ChatClient.Builder chatClientBuilder;


    @Test
    void vectorStoreTest() {

        List <Document> documents = List.of(
                new Document("Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!", Map.of("meta1", "meta1")),
                new Document("The World is Big and Salvation Lurks Around the Corner"),
                new Document("You walk forward facing the past and you turn back toward the future.", Map.of("meta2", "meta2")));

        vectorStore.add(documents);

        List<Document> results = this.vectorStore.similaritySearch(SearchRequest.query("Spring").withTopK(5));
    }

    @Test
    void readDocsWithChroma() {
        Resource resource = new ClassPathResource("docs/tax.docx");

        List<Document> documents = myTikaDocumentReader.loadText(resource);
        List<Document> splitDocs = myTokenTextSplitter.splitCustomized(documents);

        System.out.println("${splitDocs.size()} = " + splitDocs.size());

        vectorStore.add(splitDocs);
    }

    @Test
    void vectorStoreSimilaritySearchTest() {

        List<Document> documents = vectorStore.similaritySearch(SearchRequest.defaults().withTopK(5).withQuery("연소득 5000만원인 거주자의 소득세는?"));

        for (Document document : documents) {
            System.out.println(document);
        }
    }

    @Test
    void openAIChatTest() {

        ChatResponse response = chatModel.call(
                new Prompt(
                        "Generate the names of 5 famous pirates.",
                        OpenAiChatOptions.builder()
                                .withModel("gpt-4o-mini")
                                .withTemperature(0.4)
                                .build()
                ));

        String result = response.getResult().toString();

        System.out.println(result);
    }

    private String dictionaryChain(String query) {

        ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);

        chatClientBuilder.defaultAdvisors(new ReReadingAdvisor());

        ChatClient chatClient = chatClientBuilder.build();
        return chatClient.prompt()
                .user(query)
                .call().content();
    }

    @Test
    void advisorTest() {
        String query = "연소득 1억원인 직장인의 소득세는?";

        String query2 = dictionaryChain(query);

        InMemoryChatMemory inMemoryChatMemory = new InMemoryChatMemory();

        List<Message> messages = List.of(
                new UserMessage("연소득 5000만원인 직장인의 소득세는?"), new SystemMessage("소득세법 제26조에 따르면, 연소득 5,000만원인 거주자의 소득세는 기본적으로 624만원에 5,000만원을 초과하는 금액의 24%를 추가로 계산하여 산출됩니다. 따라서, 전체 소득세는 624만원 + (5,000만원 - 4,200만원) * 0.24로 계산됩니다.")
        );

        inMemoryChatMemory.add("1", messages);
        chatClientBuilder.defaultSystem("""
                    "당신은 소득세법 전문가입니다. 사용자의 소득세법에 관한 질문에 답변을 해주세요."
                    "아래에 제공된 문서를 활용해서 답변해 주시고"
                    "답변을 알 수 없다면 모른다고 답변해 주세요"
                    "답변을 제공할 때는 소득세법 (XX조)에 따르면 이라고 시작하면서 답변해주시고"
                    "2-3 문장정도의 짧은 내용의 답변을 원합니다."
                """);
        chatClientBuilder.defaultAdvisors(
                new MessageChatMemoryAdvisor(inMemoryChatMemory), // CHAT MEMORY
                QuestionAnswerAdvisor.builder(vectorStore).withOrder(1).withSearchRequest(SearchRequest.defaults().withTopK(10).withQuery(query2)).build(),
                new SimpleLoggerAdvisor());
//        builder.defaultFunctions("getBookingDetails", "changeBooking", "cancelBooking");
        ChatClient chatClient = chatClientBuilder.build();
        String answer = chatClient.prompt()
                .user(query2)
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, 1L)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100)
                )
                .call().content();
        System.out.println(answer);
    }


}