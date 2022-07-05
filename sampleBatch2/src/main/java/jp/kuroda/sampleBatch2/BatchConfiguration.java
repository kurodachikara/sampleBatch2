package jp.kuroda.sampleBatch2;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
	@Autowired
	private JobBuilderFactory jobFactory;
	@Autowired
	private StepBuilderFactory stepFactory;
	
	@Bean
	public FlatFileItemReader<Person> reader(){
		return new FlatFileItemReaderBuilder<Person>()
				.name("PersonItemReader")
				.resource(new ClassPathResource("sample-data.csv"))
				.delimited().names(new String[] {"lastName","firstName"})
				.fieldSetMapper(new BeanWrapperFieldSetMapper<Person>() {{
					setTargetType(Person.class);
				}})
				.build();
	}
	@Bean
	public PersonItemProcessor processor() {
		return new PersonItemProcessor();
	}
	@Bean
	public JdbcBatchItemWriter<Person> writer(DataSource dataSource){
		return new JdbcBatchItemWriterBuilder<Person>()
				.itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
				.sql("INSERT INTO people (last_name,first_name) VALUES (:lastName, :firstName)")
				.dataSource(dataSource)
				.build();
	}
	@Bean
	public Job importUserJob(JobListener listener,Step step1) {
		return jobFactory.get("importUserJob")
				.incrementer(new RunIdIncrementer())
				.listener(listener)
				.flow(step1)
				.end()
				.build();
	}
	@Bean
	public Step step1(JdbcBatchItemWriter<Person> writer) {
		return stepFactory.get("step1")
				.<Person, Person>chunk(10)
				.reader(reader())
				.processor(processor())
				.writer(writer)
				.build();
	}
}
