package dev.genai.sqlrag4j.config;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

import javax.sql.DataSource;

import com.mysql.cj.jdbc.MysqlDataSource;

import dev.genai.sqlrag4j.api.Assistant;
import dev.genai.sqlrag4j.core.AnnotationProcessor;
import dev.genai.sqlrag4j.exceptions.ModelNotDefinedException;
import dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;

public class SQLRagConfiguration {

	private Assistant assistant;
	private String packageToScan;
	
	public SQLRagConfiguration(String packageToScan) {
		DataSource dataSource = dataSource();
		ChatLanguageModel chatLanguageModel = chatModel();
		ContentRetriever contentRetriever = contentRetriever(dataSource, chatLanguageModel);
		assistant = assistant(chatLanguageModel, contentRetriever);
		this.packageToScan = packageToScan;
	}
    public DataSource dataSource() {
    	MysqlDataSource ds = new MysqlDataSource();
        ds.setUrl(System.getenv("sqlrag4j.ai.database.url"));
        ds.setUser(System.getenv("sqlrag4j.ai.database.user"));
        ds.setPassword(System.getenv("sqlrag4j.ai.database.password"));
        return ds;
    }

    public ChatLanguageModel chatModel() {
    	String model = System.getenv("sqlrag4j.ai.llm.model");
    	
    	if (Objects.isNull(model)) {
             throw new ModelNotDefinedException("Environment variable 'sqlrag4j.ai.llm.model' is not set or is blank.");
        }
    	
    	switch (model.toLowerCase()) {
		case "ollama":
			String url = System.getenv("sqlrag4j.ai.llm.ollama.url");
			return OllamaChatModel.builder()
	                .baseUrl(url)
	                .modelName("tinyllama")
	                .temperature(0.0)
	                .timeout(Duration.ofMinutes(10))
	                .build();
		case "openai":
			String openAIKey = System.getenv("sqlrag4j.ai.llm.openai.key");
			return OpenAiChatModel.builder()
	                .apiKey(openAIKey)
	                .modelName("gpt-4o")
	                .build();
		default:
			throw new ModelNotDefinedException(String.format("Model not defined: %s", model));
		}
    }
   
    public ContentRetriever contentRetriever(DataSource dataSource, ChatLanguageModel model) {
        return SqlDatabaseContentRetriever.builder()
                .dataSource(dataSource)
                .chatLanguageModel(model)
                .build();
    }

    public Assistant assistant(ChatLanguageModel model, ContentRetriever contentRetriever) {
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .contentRetriever(contentRetriever)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
    
    @SuppressWarnings("unchecked")
	public <T> T get() throws ClassNotFoundException, IOException {
    	AnnotationProcessor processor = new AnnotationProcessor(getAssistant());
        return (T) processor.createProxy(this.packageToScan);
    }
    
    public Assistant getAssistant() {
        return assistant;
    }
}
