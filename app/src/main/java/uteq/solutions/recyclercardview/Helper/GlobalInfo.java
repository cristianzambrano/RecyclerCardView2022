package uteq.solutions.recyclercardview.Helper;

import java.util.HashMap;
import java.util.Map;

public class GlobalInfo {
    public static final String Token="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";
    public static final String URL_CreatThread ="https://api.openai.com/v1/threads";
    public static final String Assistant_ID = "asst_dKqNiDGt1ltpNWq0hzIxuYbd";
    public static final String Instructions = "Eres un chatbot que responde preguntas sobre la Carrera de Ingeniería de Software y de la Universidad Técnica Estatal de Quevedo (UTEQ)";

    public static final String getUrlCreateMessage(String ThreadID){
        return "https://api.openai.com/v1/threads/" + ThreadID + "/messages";

    }
    public static final String getURLRunMessage(String ThreadID){
        return "https://api.openai.com/v1/threads/" + ThreadID +"/runs";
    }
    public static final String getURLCheckStatus(String ThreadID, String runId){
        return "https://api.openai.com/v1/threads/" + ThreadID + "/runs/" + runId;
    }

    public static final String getURLGetMessage(String ThreadID, String lastMessageID){
        return "https://api.openai.com/v1/threads/" + ThreadID +"/messages" +
                "?order=asc&after=" + lastMessageID;
    }

    public static Map<String, String> getAuthHearders(){
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + Token);
        headers.put("OpenAI-Beta", "assistants=v1");
        return headers;
    }

}
