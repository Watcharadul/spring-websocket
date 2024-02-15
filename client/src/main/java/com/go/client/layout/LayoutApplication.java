package com.go.client.layout;

import java.lang.reflect.Type;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import com.go.client.constance.MessageTypeEnum;
import com.go.client.constance.ProcessStatusEnum;
import com.go.client.response.MessageResponse;
import com.go.client.socket.SocketHandler;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class LayoutApplication extends Application implements Initializable{
    @FXML
    private Label labelTime;
    @FXML
    private TextField host;
    @FXML
    private TextField clientName;
    @FXML
    private Button button;
    @FXML
    private TextArea body;
    @FXML
    private TextField message;
    @FXML
    private ComboBox<SessionData> comboBoxSession;
    private SocketHandler socket = SocketHandler.getInstance();

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/page/layoutPage.fxml"));
        primaryStage.setTitle("JavaFX Socket");
        Scene scene = new Scene(loader.load(),600,400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void setBody(String message){
        Platform.runLater(() -> {
            String currentText = body.getText();
            if(!currentText.isEmpty()){
                currentText += "\n";
            }
            body.setText(currentText+message);
        });
    }

    private <T> List<T> convertJsonToList(Object obj, Class<T> clazz) {
        Gson gson = new Gson();
        Type listType = TypeToken.getParameterized(List.class, clazz).getType();
        return gson.fromJson(gson.toJson(obj), listType);
    }

    public void actionHandler(){
        if(host.getText().isEmpty() || clientName.getText().isEmpty()){
            return;
        }
        
        if(!socket.isOpen()){
            socket.setHostName(host.getText()).setName(clientName.getText()).connect();
        }else{
            socket.disconnect();
        }
    }

    public void messageHandler(){
        String msg = message.getText();
        SessionData session = Optional.ofNullable(comboBoxSession.getSelectionModel().getSelectedItem()).orElse(new SessionData());
        if(socket.isOpen() && !msg.isEmpty()){
            socket.sendMessage(session.getId(),msg);
        }
        message.clear();
    }

    @Override
    public void initialize(URL url, ResourceBundle res) {
        socket.put(ProcessStatusEnum.MESSAGE,msg->{
            MessageResponse response = (MessageResponse) msg;
            String message = response.getMessage();
            if (response.getData() instanceof List) {
                Platform.runLater(() -> {
                    comboBoxSession.getItems().clear();
                    comboBoxSession.getItems().add(SessionData.builder().id("").name("----- ALL -----").build());
                    comboBoxSession.getItems().addAll(convertJsonToList(response.getData(),SessionData.class));
                    comboBoxSession.getSelectionModel().selectFirst();
                });
            }
            if(response.getMessageType().equals(MessageTypeEnum.SYSTEM)){
                Platform.runLater(() -> labelTime.setText(message));
                return;
            }
            setBody(message);
  }).put(ProcessStatusEnum.ACTION,o->{
            clientName.setDisable(socket.isOpen());
            host.setDisable(socket.isOpen());
            comboBoxSession.setDisable(!socket.isOpen());
            message.setDisable(!socket.isOpen());
            Platform.runLater(() -> button.setText(socket.isOpen() ? "Disconnect" : "Connect"));
        });
    }
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class SessionData{
    private String id;
    private String name;
    
    @Override
    public String toString(){
        return name;
    }
}