package org.service.taxchatbot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.ChromaVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.List;

@SpringBootTest
class MyTikaDocumentReaderTest {

    @Autowired
    MyTikaDocumentReader myTikaDocumentReader;
    @Autowired
    MyTokenTextSplitter myTokenTextSplitter;
    @Autowired
    VectorStore vectorStore;



    @Test
    void readDocsWithChroma() {
        Resource resource = new ClassPathResource("docs/tax.docx");

        List<Document> documents = myTikaDocumentReader.loadText(resource);
        List<Document> splitDocs = myTokenTextSplitter.splitCustomized(documents);

        System.out.println("${splitDocs.size()} = " + splitDocs.size());

        vectorStore.add(splitDocs);
    }

    @Test
    void vectorStoreSimilaritySearch() {

        List<Document> documents = vectorStore.similaritySearch("연소득 5000만원인 거주자의 소득세는?");

        for (Document document : documents) {
            System.out.println(document);
        }
    }
}