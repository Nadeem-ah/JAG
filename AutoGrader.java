package AutoGrader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.List;

public class AutoGraderGUI extends JFrame {

    private static final long serialVersionUID = 1L;
    private JTextArea resultArea;
    private JButton loadButton;
    private JButton gradeButton;
    private JTextField filePathField;

    public AutoGraderGUI() {
        setTitle("Rubric-Based AutoGrader");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        filePathField = new JTextField(30);
        panel.add(filePathField);

        loadButton = new JButton("Load Code");
        loadButton.addActionListener(new LoadButtonListener());
        panel.add(loadButton);

        gradeButton = new JButton("Grade Code");
        gradeButton.addActionListener(new GradeButtonListener());
        panel.add(gradeButton);

        add(panel, BorderLayout.NORTH);
    }

    private class LoadButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Java File or Directory");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                filePathField.setText(selectedFile.getAbsolutePath());
            }
        }
    }

    private class GradeButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String filePath = filePathField.getText();
            if (filePath.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please select a Java file or directory.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            resultArea.setText("Grading in progress...\n");

            SwingWorker<Void, String> graderWorker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    File source = new File(filePath);
                    if (!source.exists()) {
                        publish("Error: File or directory not found.");
                        return null;
                    }

                    File sourceDirectory = source.isDirectory() ? source : source.getParentFile();
                    File[] javaFiles = sourceDirectory.listFiles((dir, name) -> name.endsWith(".java"));

                    if (javaFiles == null || javaFiles.length == 0) {
                        publish("Error: No Java files found to compile.");
                        return null;
                    }

                    for (File javaFile : javaFiles) {
                        try {
                            publish("\nGrading file: " + javaFile.getName());

                            // Compilation
                            String compileResult = compileFile(javaFile, sourceDirectory);
                            publish(compileResult);

                            if (compileResult.contains("Compilation Failed")) continue;

                            // Execution
                            String executeResult = executeClass(javaFile, sourceDirectory);
                            publish(executeResult);

                            if (executeResult.contains("Execution Failed")) continue;

                            // Rubric Evaluation
                            int correctness = evaluateCorrectness(executeResult, "Expected Output Here");
                            int codeStyle = evaluateCodeStyle(javaFile);
                            int documentation = evaluateDocumentation(javaFile);
                            int fileHandling = evaluateFileHandling(javaFile);
                            int logicDesign = evaluateLogicDesign(javaFile);

                            int totalScore = correctness + codeStyle + documentation + fileHandling + logicDesign;

                            // Display Rubric Results
                            publish("\nRubric Results for " + javaFile.getName() + ":");
                            publish("Correctness: " + correctness + "/20");
                            publish("Code Style: " + codeStyle + "/20");
                            publish("Documentation: " + documentation + "/20");
                            publish("File Handling: " + fileHandling + "/20");
                            publish("Logic Design: " + logicDesign + "/20");
                            publish("Total Score: " + totalScore + "/100");
                        } catch (Exception ex) {
                            publish("Error grading file: " + ex.getMessage());
                        }
                    }

                    return null;
                }

                @Override
                protected void process(List<String> chunks) {
                    for (String message : chunks) {
                        resultArea.append(message + "\n");
                    }
                }

                @Override
                protected void done() {
                    resultArea.append("\nGrading process complete.\n");
                }
            };

            graderWorker.execute();
        }
    }

    // Compile the given Java file
    private static String compileFile(File javaFile, File sourceDirectory) throws IOException, InterruptedException {
        String compileCommand = "javac \"" + javaFile.getAbsolutePath() + "\"";
        ProcessBuilder pb = new ProcessBuilder(compileCommand.split(" "));
        pb.directory(sourceDirectory);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = readProcessOutput(process);
        int exitCode = process.waitFor();
        return exitCode == 0 ? "Compilation succeeded." : "Compilation Failed:\n" + output;
    }

    // Execute the compiled class file
    private static String executeClass(File javaFile, File sourceDirectory) throws IOException, InterruptedException {
        String className = javaFile.getName().replace(".java", "");
        String executeCommand = "java -cp \"" + sourceDirectory.getAbsolutePath() + "\" " + className;
        ProcessBuilder pb = new ProcessBuilder(executeCommand.split(" "));
        pb.directory(sourceDirectory);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = readProcessOutput(process);
        int exitCode = process.waitFor();
        return exitCode == 0 ? "Execution succeeded:\n" + output : "Execution Failed:\n" + output;
    }

    // Read output of a process
    private static String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    // Rubric evaluations
    private static int evaluateCorrectness(String output, String expectedOutput) {
        return output.trim().equals(expectedOutput.trim()) ? 20 : 0;
    }

    private static int evaluateCodeStyle(File javaFile) throws IOException {
        List<String> lines = Files.readAllLines(javaFile.toPath());
        return lines.stream().anyMatch(line -> line.contains(";")) ? 20 : 0;
    }

    private static int evaluateDocumentation(File javaFile) throws IOException {
        List<String> lines = Files.readAllLines(javaFile.toPath());
        return lines.stream().anyMatch(line -> line.contains("/**")) ? 20 : 0;
    }

    private static int evaluateFileHandling(File javaFile) throws IOException {
        List<String> lines = Files.readAllLines(javaFile.toPath());
        return lines.stream().anyMatch(line -> line.contains("File") || line.contains("BufferedReader")) ? 20 : 0;
    }

    private static int evaluateLogicDesign(File javaFile) throws IOException {
        List<String> lines = Files.readAllLines(javaFile.toPath());
        return lines.stream().anyMatch(line -> line.contains("if") || line.contains("while")) ? 20 : 0;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AutoGraderGUI frame = new AutoGraderGUI();
            frame.setVisible(true);
        });
    }
}
