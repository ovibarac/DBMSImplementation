package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

  public static void main(String[] args) {
    try (Socket socket = new Socket("localhost", 12345)) {
      System.out.println("Connected to server.");

      BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

      Scanner scanner = new Scanner(System.in);
      String command;

      while (true) {
        System.out.print("Enter command (or type 'exit' to quit): ");
        command = scanner.nextLine();

        sendCommand(output, command);

        if (command.equalsIgnoreCase("exit")) {
          System.out.println("Exiting...");
          break;
        }

        String response = receiveResponse(input);
        System.out.println(response);
      }

      scanner.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void sendCommand(PrintWriter output, String command) {
    output.println(command);
  }

  private static String receiveResponse(BufferedReader input) throws IOException {
    return input.readLine();
  }
}
