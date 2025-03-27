package leapro.io.convagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("leapro.io")
@SpringBootApplication
public class ConversationAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConversationAgentApplication.class, args);
	}

}
