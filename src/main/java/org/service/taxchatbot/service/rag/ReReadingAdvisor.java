package org.service.taxchatbot.service.rag;

import org.springframework.ai.chat.client.advisor.api.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

public class ReReadingAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {


    private AdvisedRequest before(AdvisedRequest advisedRequest) {

//        Map<String, Object> wordDictionary = Map.of("직장인", "거주자");
        Map<String, Object> advisedUserParams = new HashMap<>(advisedRequest.userParams());
        advisedUserParams.put("input", advisedRequest.userText());

        return AdvisedRequest.from(advisedRequest)
                .withUserText("""
                        사용자의 질문을 보고, 우리의 사전을 참고해서 사용자의 질문을 변경해주세요.
                        만약 변경할 필요가 없다고 판단된디면, 사용자의 질문을 변경하지 않아도 됩니다.
                        그런 경우에는 질문만 반환해주세요.
                        사전: 직장인, 거주자
						질문: {input}
						""")
                .withUserParams(advisedUserParams)
                .build();
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        return chain.nextAroundCall(this.before(advisedRequest));
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        return chain.nextAroundStream(this.before(advisedRequest));
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}