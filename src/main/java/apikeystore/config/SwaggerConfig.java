package apikeystore.config;

import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.PathSelectors.regex;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Predicate;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.DocExpansion;
import springfox.documentation.swagger.web.OperationsSorter;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger.web.UiConfigurationBuilder;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

	@Bean
	public Docket api() {
		return new Docket(DocumentationType.SWAGGER_2).groupName("api-keystore").apiInfo(apiInfo()).select()
				.paths(paths()).build();
	}

	@SuppressWarnings("unchecked")
	private Predicate<String> paths() {
		return or(regex("/keys.*"));
	}

	private ApiInfo apiInfo() {
		return new ApiInfoBuilder().title("OAuth API Keystore")
				.description("OAuth token and credentials keystore for API services.").version("0.3").build();
	}

	@Bean
	UiConfiguration uiConfig() {
		return UiConfigurationBuilder.builder().docExpansion(DocExpansion.LIST).operationsSorter(OperationsSorter.ALPHA)
				.build();
	}
}
