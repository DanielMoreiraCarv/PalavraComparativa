import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

public class AnaliseEGerarGrafico {

    private static final String INPUT_CSV = "resultados_busca.csv";
    private static final String OUTPUT_CSV = "medias_aggregadas.csv";
    private static final String OUTPUT_PNG = "medias_desempenho.png";

    static class DadosAgregados {
        long tempoTotal = 0;
        int contagem = 0;
        long ocorrencias = 0;
    }

    public static void main(String[] args) {
        System.out.println("Iniciando análise e geração de gráfico (Java/JFreeChart) ...");

        if (!Files.exists(Paths.get(INPUT_CSV))) {
            System.err.println("Arquivo não encontrado: " + INPUT_CSV);
            return;
        }

        Map<String, DadosAgregados> agregacao = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(INPUT_CSV))) {
            String linha = br.readLine(); // cabeçalho
            if (linha == null) {
                System.err.println("CSV vazio: " + INPUT_CSV);
                return;
            }

            int linhaIdx = 1;
            while ((linha = br.readLine()) != null) {
                linhaIdx++;
                if (linha.trim().isEmpty()) continue;
                String[] partes = linha.split(",", -1);
                if (partes.length < 4) {
                    System.err.println("Linha ignorada (formato inesperado) [" + linhaIdx + "]: " + linha);
                    continue;
                }
                String arquivo = partes[0].trim();
                String metodo = partes[1].trim();
                long ocorrencias;
                long tempoMs;
                try {
                    ocorrencias = Long.parseLong(partes[2].trim());
                    tempoMs = Long.parseLong(partes[3].trim());
                } catch (NumberFormatException nfe) {
                    System.err.println("Linha com número inválido ignorada [" + linhaIdx + "]: " + linha);
                    continue;
                }

                String chave = arquivo + "|" + metodo;
                DadosAgregados d = agregacao.computeIfAbsent(chave, k -> new DadosAgregados());
                d.tempoTotal += tempoMs;
                d.contagem++;
                d.ocorrencias = ocorrencias;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (agregacao.isEmpty()) {
            System.err.println("Nenhum dado agregado — verifique o CSV.");
            return;
        }

        try (PrintWriter pw = new PrintWriter(new File(OUTPUT_CSV))) {
            pw.println("Arquivo,Metodo,TempoMedio_ms,Amostras,Ocorrencias_exemplo");
            for (String chave : agregacao.keySet().stream().sorted().collect(Collectors.toList())) {
                DadosAgregados d = agregacao.get(chave);
                String[] parts = chave.split("\\|", 2);
                String arquivo = parts[0];
                String metodo = parts.length > 1 ? parts[1] : "";
                double media = (double) d.tempoTotal / d.contagem;
                pw.printf("%s,%s,%.2f,%d,%d%n", arquivo, metodo, media, d.contagem, d.ocorrencias);
            }
            System.out.println("CSV de médias gravado em: " + OUTPUT_CSV);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (String chave : agregacao.keySet().stream().sorted().collect(Collectors.toList())) {
            DadosAgregados d = agregacao.get(chave);
            String[] parts = chave.split("\\|", 2);
            String arquivo = parts[0];
            String metodo = parts.length > 1 ? parts[1] : "";
            double media = (double) d.tempoTotal / d.contagem;
            String label = arquivo + " | " + metodo;
            dataset.addValue(media, "Tempo médio (ms)", label);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Tempo médio por Arquivo e Método",
                "Cenário",
                "Tempo médio (ms)",
                dataset
        );

        try {
            int width = Math.max(800, agregacao.size() * 120);
            int height = 600;
            ChartUtils.saveChartAsPNG(new File(OUTPUT_PNG), chart, width, height);
            System.out.println("Gráfico salvo em: " + OUTPUT_PNG);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Falha ao salvar PNG.");
        }

        System.out.println("Concluído.");
    }
}
