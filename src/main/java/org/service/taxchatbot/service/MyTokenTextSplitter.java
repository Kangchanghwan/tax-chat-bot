package org.service.taxchatbot.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class MyTokenTextSplitter {

    public List<Document> splitCustomized(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter(1500, 400, 10, 5000, false);
        return splitter.apply(documents);
    }
}