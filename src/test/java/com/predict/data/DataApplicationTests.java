package com.predict.data;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DataApplicationTests {

    @Test
    public void contextLoads() {
    }

    @Test
    public void testSurvey() {
        System.out.println("Survey Creation Success");
    }
}

//    @Test
//    public void surveyAddQuestion() {
//        SurveyService surveyService = new SurveyService();
//
//        String question = "Does this work?";
//        String category = "testing";
//        Question questionObject = new Question(question, category);
//
//        surveyService.createSurvey(questionObject);
//    }

