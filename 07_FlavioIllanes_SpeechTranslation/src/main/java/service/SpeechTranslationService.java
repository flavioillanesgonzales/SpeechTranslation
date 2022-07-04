package service;
//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE.md file in the project root for full license information.
//

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

// <toplevel>
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.*;
import com.microsoft.cognitiveservices.speech.translation.*;
import service.WavStream;
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
    public static String apiKey = "0608d66b45184916a656e92b11af6633";
    public static String region = "eastus";

    public SpeechTranslationService() {
    }

    // Translation from microphone.
    public static String cadena;

    public static void translationWithMicrophoneAsync(String fromLanguage, String targetLanguage, String voice) throws InterruptedException, ExecutionException, IOException {
        //<TraducciónConMicrophoneAsync>
        // Crea una instancia de una configuración de traducción de voz con especificado
        // clave de suscripción y región de servicio. Reemplace con su propia clave de suscripción
        // y región de servicio (por ejemplo, "westus").
        config = SpeechTranslationConfig.fromSubscription(apiKey, region);

        // Establece los idiomas de origen y de destino.
//        String fromLanguage = "es-PE";
        config.setSpeechRecognitionLanguage(fromLanguage);
//        config.addTargetLanguage("en-US");
        config.addTargetLanguage(targetLanguage); // Establece el nombre de voz de la salida de síntesis.
        
        voice = "Microsoft Server Speech Text to Speech Voice (" +targetLanguage+", "+voice+")"; 
        System.out.println("VOz es esta: " + voice);
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
                    } catch (LineUnavailableException e1) {
                        e1.printStackTrace();
                    } catch (UnsupportedAudioFileException e1) {
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            });

            recognizer.canceled.addEventListener((s, e) -> {
                System.out.println("CANCELED: Reason=" + e.getReason());

                if (e.getReason() == CancellationReason.Error) {
                    System.out.println("CANCELED: ErrorCode=" + e.getErrorCode());
                    System.out.println("CANCELED: ErrorDetails=" + e.getErrorDetails());
                    System.out.println("CANCELED: Did you update the subscription info?");
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

//            System.out.println("Presiona cualquier tecla para parar");
//            new Scanner(System.in).nextLine();
//            System.out.println("Entonces se va a parar");
//            recognizer.stopContinuousRecognitionAsync().get();
        }
        // </TraducciónConMicrophoneAsync>
    }

    // Traducción utilizando la entrada de archivos.
    // <TranslationWithFileAsync>
    private static Semaphore stopTranslationWithFileSemaphore;

    public static void translationWithFileAsync(String ruta) throws InterruptedException, ExecutionException {
        stopTranslationWithFileSemaphore = new Semaphore(0);

        config = SpeechTranslationConfig.fromSubscription(apiKey, region);
        // Crea una instancia de una configuración de traducción de voz con especificado
        // clave de suscripción y región de servicio. Reemplace con su propia clave de suscripción
        // y región de servicio (por ejemplo, "westus").

        // Establece los idiomas de origen y de destino
        String fromLanguage = "es-PE";
        config.setSpeechRecognitionLanguage(fromLanguage);
        config.addTargetLanguage("en");
//        config.addTargetLanguage("fr");

        // Crea un reconocedor de traducción utilizando el archivo como entrada de audio.
        // Reemplácelo con su propio nombre de archivo de audio.
        AudioConfig audioInput = AudioConfig.fromWavFileInput(ruta);
        recognizer = new TranslationRecognizer(config, audioInput);
        {
            // se suscribe a los eventos.
            recognizer.recognizing.addEventListener((s, e) -> {
                System.out.println("RECOGNIZING in '" + fromLanguage + "': Text=" + e.getResult().getText());

                Map<String, String> map = e.getResult().getTranslations();
                for (String element : map.keySet()) {
                    System.out.println("    TRANSLATING into '" + element + "'': " + map.get(element));
                }
            });

            recognizer.recognized.addEventListener((s, e) -> {
                if (e.getResult().getReason() == ResultReason.TranslatedSpeech) {
                    System.out.println("RECOGNIZED in '" + fromLanguage + "': Text=" + e.getResult().getText());

                    Map<String, String> map = e.getResult().getTranslations();
                    for (String element : map.keySet()) {
                        System.out.println("    TRANSLATED into '" + element + "'': " + map.get(element));
                    }
                }
                if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                    System.out.println("RECOGNIZED: Text=" + e.getResult().getText());
                    System.out.println("    Speech not translated.");
                } else if (e.getResult().getReason() == ResultReason.NoMatch) {
                    System.out.println("NOMATCH: Speech could not be recognized.");
                }
            });

            recognizer.canceled.addEventListener((s, e) -> {
                System.out.println("CANCELED: Reason=" + e.getReason());

                if (e.getReason() == CancellationReason.Error) {
                    System.out.println("CANCELED: ErrorCode=" + e.getErrorCode());
                    System.out.println("CANCELED: ErrorDetails=" + e.getErrorDetails());
                    System.out.println("CANCELED: Did you update the subscription info?");
                }

                stopTranslationWithFileSemaphore.release();;
            });

            recognizer.sessionStarted.addEventListener((s, e) -> {
                System.out.println("\nSession started event.");
            });

            recognizer.sessionStopped.addEventListener((s, e) -> {
                System.out.println("\nSession stopped event.");

                // Detiene la traducción cuando se detecta la detención de la sesión.
                System.out.println("\nStop translation.");
                stopTranslationWithFileSemaphore.release();;
            });

            // Inicia el reconocimiento continuo. Utiliza StopContinuousRecognitionAsync() para detener el reconocimiento.
            System.out.println("Start translation...");
            recognizer.startContinuousRecognitionAsync().get();

            // Espera a que se complete.
            stopTranslationWithFileSemaphore.acquire();;

            // Detiene la traducción.
            recognizer.stopContinuousRecognitionAsync().get();
        }
    }
    // </TranslationWithFileAsync>

    // Traducción usando flujo de audio.
    private static Semaphore stopTranslationWithAudioStreamSemaphore;

    public static void translationWithAudioStreamAsync() throws InterruptedException, ExecutionException, FileNotFoundException {
        stopTranslationWithAudioStreamSemaphore = new Semaphore(0);

        // Crea una instancia de una configuración de traducción de voz con especificado
        // clave de suscripción y región de servicio. Reemplace con su propia clave de suscripción
        // y región de servicio (por ejemplo, "westus").
        config = SpeechTranslationConfig.fromSubscription(apiKey, region);

        // Establece los idiomas de origen y de destino
        String fromLanguage = "en-US";
        config.setSpeechRecognitionLanguage(fromLanguage);
        config.addTargetLanguage("de");
        config.addTargetLanguage("fr");

        // Cree una transmisión de audio a partir de un archivo wav.
        // Reemplácelo con su propio nombre de archivo de audio.
        PullAudioInputStreamCallback callback = new WavStream(new FileInputStream("YourAudioFile.wav"));
        AudioConfig audioInput = AudioConfig.fromStreamInput(callback);

        // Crea un reconocedor de traducción usando flujo de audio como entrada.
        recognizer = new TranslationRecognizer(config, audioInput);
        {
            // Se suscribe a eventos.
            recognizer.recognizing.addEventListener((s, e) -> {
                System.out.println("RECOGNIZING in '" + fromLanguage + "': Text=" + e.getResult().getText());

                Map<String, String> map = e.getResult().getTranslations();
                for (String element : map.keySet()) {
                    System.out.println("    TRANSLATING into '" + element + "'': " + map.get(element));
                }
            });

            recognizer.recognized.addEventListener((s, e) -> {
                if (e.getResult().getReason() == ResultReason.TranslatedSpeech) {
                    System.out.println("RECOGNIZED in '" + fromLanguage + "': Text=" + e.getResult().getText());

                    Map<String, String> map = e.getResult().getTranslations();
                    for (String element : map.keySet()) {
                        System.out.println("    TRANSLATED into '" + element + "'': " + map.get(element));
                    }
                }
                if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                    System.out.println("RECOGNIZED: Text=" + e.getResult().getText());
                    System.out.println("    Speech not translated.");
                } else if (e.getResult().getReason() == ResultReason.NoMatch) {
                    System.out.println("NOMATCH: Speech could not be recognized.");
                }
            });

            recognizer.canceled.addEventListener((s, e) -> {
                System.out.println("CANCELED: Reason=" + e.getReason());

                if (e.getReason() == CancellationReason.Error) {
                    System.out.println("CANCELED: ErrorCode=" + e.getErrorCode());
                    System.out.println("CANCELED: ErrorDetails=" + e.getErrorDetails());
                    System.out.println("CANCELED: Did you update the subscription info?");
                }

                stopTranslationWithAudioStreamSemaphore.release();
            });

            recognizer.sessionStarted.addEventListener((s, e) -> {
                System.out.println("\nSession started event.");
            });

            recognizer.sessionStopped.addEventListener((s, e) -> {
                System.out.println("\nSession stopped event.");

                // Detiene la traducción cuando se detecta la detención de la sesión.
                System.out.println("\nStop translation.");
                stopTranslationWithAudioStreamSemaphore.release();
            });

            // Inicia el reconocimiento continuo. Utiliza StopContinuousRecognitionAsync() para detener el reconocimiento.
            System.out.println("Start translation...");
            recognizer.startContinuousRecognitionAsync().get();

            //Espera a que se complete.
            stopTranslationWithAudioStreamSemaphore.acquire();

            // Detiene la traducción.
            recognizer.stopContinuousRecognitionAsync().get();
        }
    }

}
