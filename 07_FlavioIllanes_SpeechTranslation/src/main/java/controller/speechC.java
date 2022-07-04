package controller;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import model.SpeechM;
import service.SpeechTranslationService;

@Named(value = "speechC")
@SessionScoped
public class speechC implements Serializable {

    private SpeechTranslationService speech = new SpeechTranslationService();
    private String cadena = "Aqui aparecera lo que habla";
    private boolean inab = false;
    private boolean img = true;
    private SpeechM sp = new SpeechM();

    public void grabarTraducir() throws InterruptedException, ExecutionException, IOException {
        try {
            inab = true;
            String voice = "";
            switch (sp.getTraducir()) {
                case "en-US":
                    voice = "AriaNeural";
                    break;
                case "es-PE":
                    voice = "AlexNeural";
                    break;
                case "pt-BR":
                    voice = "AntonioNeural";
                    break;
//                case "":
//                    break;
            }
            speech.translationWithMicrophoneAsync(sp.getTraduccion(), sp.getTraducir(),voice);
        } catch (IOException | InterruptedException | ExecutionException e) {
            System.out.println("Error en traducirMicro: " + e.getMessage());
        }
    }

    public void parar() throws InterruptedException, ExecutionException {
        try {

            inab = false;
            speech.recognizer.stopContinuousRecognitionAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Error en paraC " + e.getMessage());
        }

    }

    public void prueba() {
        cadena = speech.cadena;
    }

    public boolean isInab() {
        return inab;
    }

    public void setInab(boolean inab) {
        this.inab = inab;
    }

    public SpeechTranslationService getSpeech() {
        return speech;
    }

    public void setSpeech(SpeechTranslationService speech) {
        this.speech = speech;
    }

    public String getCadena() {
        return cadena;
    }

    public void setCadena(String cadena) {
        this.cadena = cadena;
    }

    public SpeechM getSp() {
        return sp;
    }

    public void setSp(SpeechM sp) {
        this.sp = sp;
    }

}
