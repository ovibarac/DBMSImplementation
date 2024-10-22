package org.example;

import org.example.services.DatabaseService;
import org.example.services.IndexService;
import org.example.services.TableService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static DatabaseService databaseService = new DatabaseService();
    private static TableService tableService = new TableService();
    private static IndexService indexService = new IndexService();
    private static SQLCommandParser commandParser = new SQLCommandParser();

    public static void main(String[] args) {
      try (ServerSocket serverSocket = new ServerSocket(12345)) {
        System.out.println("Database Server is running...");

        while (true) {
          Socket clientSocket = serverSocket.accept();
          handleClient(clientSocket);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    private static void handleClient(Socket clientSocket) {
      try (BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
           PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true)) {

        String sqlCommand;
        while ((sqlCommand = input.readLine()) != null) {
          System.out.println("Received SQL command: " + sqlCommand);

          if (sqlCommand.equalsIgnoreCase("exit")) {
            output.println("Goodbye!");
            break;
          }

          String response = commandParser.parseAndExecute(sqlCommand, databaseService, tableService, indexService);
          output.println(response);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
