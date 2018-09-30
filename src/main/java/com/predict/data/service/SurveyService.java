package com.predict.data.service;

import br.com.devfast.jsurveymonkey.app.SurveyConfig;
import br.com.devfast.jsurveymonkey.enums.StatusSurveyResponse;
import br.com.devfast.jsurveymonkey.request.CreateCollectorRequest;
import br.com.devfast.jsurveymonkey.request.CreateSurveyRequest;
import br.com.devfast.jsurveymonkey.response.CreateCollectorResponse;
import br.com.devfast.jsurveymonkey.response.CreateSurveyResponse;
import br.com.devfast.jsurveymonkey.services.SurveyMonkeyService;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.predict.data.controller.DatabaseController;
import com.predict.data.entity.Question;
import com.predict.data.entity.builder.CreatePageResponseBuilder;
import com.predict.data.entity.builder.CreateQuestionResponseBuilder;
import com.predict.data.entity.request.CreatePageRequest;
import com.predict.data.entity.request.CreateQuestionRequest;
import com.predict.data.entity.response.CreatePageResponse;
import com.predict.data.entity.response.CreateQuestionResponse;
import com.predict.data.util.ConfigManager;
import com.predict.data.util.EmailManager;
import java.net.URI;
import java.util.Date;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("surveyService")
public class SurveyService extends SurveyMonkeyService {

  private static final Logger logger = LoggerFactory.getLogger(QuestionService.class);

  private static final String API_AUTH_TOKEN;

  static {
    API_AUTH_TOKEN = ConfigManager.getProperty("api_auth_token");
  }

  private FirebaseDatabase db;
  private UserService userService;

  @Autowired
  public SurveyService(DatabaseController databaseController, UserService userService) {
    super(API_AUTH_TOKEN);
    logger.debug("SurveyService initializing");
    this.userService = userService;
    try {
      this.db = databaseController.getDb();
      initializeListeners();
    } catch (Exception ex) {
      logger.error("Failed to initialize database: ", ex);
    }
  }

  private CreateSurveyResponse createSurvey() {
    logger.debug("createSurvey SurveyService");
    CreateSurveyRequest createSurveyRequest = new CreateSurveyRequest();
    createSurveyRequest.setTitle("Predict Question");
    createSurveyRequest.setNickname("New question from Predict!");
    createSurveyRequest.setAuthenticationToken(API_AUTH_TOKEN);

    return super.createSurvey(createSurveyRequest);
  }

  private CreatePageResponse addPage(CreatePageRequest request) {
    try {
      CloseableHttpClient httpClient = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost(new URI(SurveyConfig.ENDPOINT_V3 + SURVEY_SERVICE
          + "/" + request.getSurveyId() + "/pages"));

      setRequestAuthentication(httpPost, request.getAuthenticationToken());
      setRequestBody(httpPost, request.getJsonBody());

      CloseableHttpResponse response = httpClient.execute(httpPost);
      String result = EntityUtils.toString(response.getEntity());

      setResponse(result);
      return new CreatePageResponseBuilder(result).getResponse();

    } catch (Exception e) {
      return new CreatePageResponse(StatusSurveyResponse.ERROR, e.getMessage());

    }
  }

  private CreateQuestionResponse addQuestion(CreateQuestionRequest request) {
    try {

      CloseableHttpClient httpClient = HttpClients.createDefault();
      HttpPost httpPost = new HttpPost(new URI(request.getHref() + "/questions"));
      logger.debug(httpPost.toString());

      setRequestAuthentication(httpPost, request.getAuthenticationToken());
      setRequestBody(httpPost, request.getJsonBody());

      CloseableHttpResponse response = httpClient.execute(httpPost);
      //.debug(response.toString());
      String result = EntityUtils.toString(response.getEntity());

      setResponse(result);
      return new CreateQuestionResponseBuilder(result).getResponse();

    } catch (Exception e) {
      return new CreateQuestionResponse(StatusSurveyResponse.ERROR, e.getMessage());

    }
  }

  private void initializeListeners() {
    logger.debug("initializeListeners()");

    db.getReference("questions/").addChildEventListener(new ChildEventListener() {
      @Override
      public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
        logger.debug("onChildAdded()");
        Question question = dataSnapshot.getValue(Question.class);

        long currentTime = new Date().getTime();
        long sendTime = new Date(question.getToSend()).getTime();
        if (currentTime < sendTime) {
          return;
        }

        try {
          CreateSurveyResponse createSurveyResponse = createSurvey();
          question.setSurveyId(createSurveyResponse.getId());

          CreatePageRequest pageRequest = new CreatePageRequest(createSurveyResponse.getId());
          pageRequest.setAuthenticationToken(API_AUTH_TOKEN);

          CreatePageResponse createPageResponse = addPage(pageRequest);
          logger.debug("Create Page Response: " + createPageResponse.getResponseStatus());

          CreateQuestionRequest questionRequest = new CreateQuestionRequest(question);
          questionRequest.setHref(createPageResponse.getHref());
          questionRequest.setAuthenticationToken(API_AUTH_TOKEN);

          CreateQuestionResponse createQuestionResponse = addQuestion(questionRequest);

          logger.debug("Create Question Response: " + createQuestionResponse.getResponseStatus());

          CreateCollectorRequest createCollectorRequest = new CreateCollectorRequest();
          createCollectorRequest.setType("weblink");
          createCollectorRequest.setAuthenticationToken(API_AUTH_TOKEN);

          CreateCollectorResponse createCollectorResponse = createCollector(createCollectorRequest);
          logger.debug("Create Collector Response: " + createCollectorResponse.getResponseStatus());

          createCollectorResponse.getUrl();

          EmailManager emailManager = new EmailManager(userService);
          emailManager.sendEmailToAllUsers(createSurveyResponse.getSummary_url());
        } catch (Exception e) {
          logger.error("Error creating survey");
        }
      }

      @Override
      public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {
      }

      @Override
      public void onChildRemoved(DataSnapshot dataSnapshot) {
      }

      @Override
      public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {
      }

      @Override
      public void onCancelled(DatabaseError databaseError) {
        logger.error("Error in surveyservice");
      }
    });

    logger.debug("Got past onChildAdded listener");

  }

//
//    public void fetchSurvey() {
//        GetSurveyRequest getSurveyRequest = new GetSurveyRequest("ID_SURVEY");
//        GetSurveyResponse getSurveyResponse = surveyMonkeyService.getSurvey(getSurveyRequest);
//    }
}