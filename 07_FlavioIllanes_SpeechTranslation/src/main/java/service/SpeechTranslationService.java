package service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.ByteArrayInputStream;

// <toplevel>
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.translation.*;
// </toplevel>

@SuppressWarnings("resource") // scanner
public class SpeechTranslationService {

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        String fromLanguage = "es-PE";
        String targetLanguage = "en-US";
        String voice = "AriaNeural";
        translationWithMicrophoneAsync(fromLanguage,targetLanguage,voice);
        System.out.println("cadena es: " + cadena);

    }

    public static SpeechTranslationConfig config;
    public static TranslationRecognizer recognizer;
    public static String apiKey = "84f191573e9e41b6a75b9ebe04f6e968";
    public static String region = "eastus";

    public SpeechTranslationService() {
    }

    // Translation from microphone.
    public static String cadena = "Aqui aparecera lo que habla";

    public static void translationWithMicrophoneAsync(String fromLanguage, String targetLanguage, String voice) throws InterruptedException, ExecutionException, IOException {
        //<TraducciónConMicrophoneAsync>
        // Crea una instancia de una configuración de traducción de voz con especificado
        // clave de suscripción y región de servicio. Reemplace con su propia clave de suscripción
        // y región de servicio (por ejemplo, "eastus").
        config = SpeechTranslationConfig.fromSubscription(apiKey, region);
        // Establece los idiomas de origen y de destino.
//        String fromLanguage = "es-PE";
        config.setSpeechRecognitionLanguage(fromLanguage);
//        config.addTargetLanguage("en-US");
        config.addTargetLanguage(targetLanguage); // Establece el nombre de voz de la salida de síntesis.
        
        voice = "Microsoft Server Speech Text to Speech Voice (" +targetLanguage+", "+voice+")"; 
        config.setVoiceName(voice);
        
        // Crea un reconocedor de traducción usando el micrófono como entrada de audio.
        recognizer = new TranslationRecognizer(config);
        {
            // Se suscribe a eventos.
            recognizer.recognizing.addEventListener((s, e) -> {
                System.out.println("RECONOCIENDO en '" + fromLanguage + "': Texto =" + e.getResult().getText());

                Map<String, String> map = e.getResult().getTranslations();
                for (String element : map.keySet()) {
                    System.out.println("    TRADUCIENDO dentro '" + element + "'': " + map.get(element));
                    cadena = map.get(element);
                }
            });

            recognizer.recognized.addEventListener((s, e) -> {
                if (e.getResult().getReason() == ResultReason.TranslatedSpeech) {
                    System.out.println("RECONOCIENDO en  '" + fromLanguage + "': Texto =" + e.getResult().getText());

                    Map<String, String> map = e.getResult().getTranslations();
                    for (String element : map.keySet()) {
                        System.out.println("    TRADUCIENDO dentro (elemento es) '" + element + "'': " + map.get(element));
                        cadena = map.get(element);
                    }
                }
                if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                    System.out.println("RECONOCIENDO: Texto =" + e.getResult().getText());
                    System.out.println("    Discurso no traducido.");
                } else if (e.getResult().getReason() == ResultReason.NoMatch) {
                    System.out.println("NO COINCIDIR: No se pudo reconocer el habla.");
                }
            });

            recognizer.synthesizing.addEventListener((s, e) -> {
                byte[] data = e.getResult().getAudio();

                System.out.println("Synthesis result received. Size of audio data: " + data.length);

                //Reproduzca los datos TTS de obtuvimos más que el encabezado wav.
                if (data != null && data.length > 44) {
                    try {
                        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(data);
                        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(arrayInputStream);
                        AudioFormat audioFormat = audioInputStream.getFormat();
                        DataLine.Info info = new DataLine.Info(Clip.class, audioFormat);
                        Clip clip = (Clip) AudioSystem.getLine(info);

                        clip.open(audioInputStream);
                        clip.start();
                    } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e1) {
                    }
                }
            });

            recognizer.canceled.addEventListener((s, e) -> {
                System.out.println("CANCELED: Reason=" + e.getReason());

                if (e.getReason() == CancellationReason.Error) {
                    System.out.println("CANCELADO: Codigo Error=" + e.getErrorCode());
                    System.out.println("CANCELADO: Detalles de Error=" + e.getErrorDetails());
                    System.out.println("CANCELADO: Tienes suscripicion actualizada?");
                }
            });

            recognizer.sessionStarted.addEventListener((s, e) -> {
                System.out.println("\nSession started event.");
            });

            recognizer.sessionStopped.addEventListener((s, e) -> {
                System.out.println("\nSession stopped event.");
            });

            // Inicia el reconocimiento continuo. Utiliza StopContinuousRecognitionAsync() para detener el reconocimiento.
            System.out.println("Diga algo...");
            recognizer.startContinuousRecognitionAsync().get();

        }
        // </TraducciónConMicrophoneAsync>
    }

}
