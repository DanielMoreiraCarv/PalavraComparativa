import org.jocl.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import static org.jocl.CL.*;

public class BuscaPalavraComparativa {

    // Kernel OpenCL LIMPO (Sem comentários para evitar erros de compilação na GPU)
    private static String programSource =
            "kernel void countWords(global const char* text, int textLen, \n" +
                    "                         global const char* pattern, int patternLen, \n" +
                    "                         global int* output) { \n" +
                    "    int gid = get_global_id(0); \n" +
                    "    if (gid >= textLen - patternLen + 1) return; \n" +
                    "    \n" +
                    "    int match = 1; \n" +
                    "    for (int i = 0; i < patternLen; i++) { \n" +
                    "        if (text[gid + i] != pattern[i]) { \n" +
                    "            match = 0; \n" +
                    "            break; \n" +
                    "        } \n" +
                    "    } \n" +
                    "    \n" +
                    "    if (match) { \n" +
                    "        atomic_inc(output); \n" +
                    "    } \n" +
                    "}";

    public static void main(String[] args) {
        // Arquivos de entrada (nomes conforme upload)
        String[] arquivos = {"MobyDick-217452.txt", "DonQuixote-388208.txt", "Dracula-165307.txt"};
        String palavraAlvo = "whale"; // Palavra para busca (case insensitive nas implementações Java)
        int numExecucoes = 3;

        List<Resultado> resultados = new ArrayList<>();

        System.out.println("Iniciando Benchmarks...");

        for (String arquivo : arquivos) {
            try {
                String conteudo = new String(Files.readAllBytes(Paths.get(arquivo)));
                System.out.println("\nProcessando: " + arquivo + " (" + conteudo.length() + " chars)");

                // --- Serial CPU ---
                for (int i = 0; i < numExecucoes; i++) {
                    long inicio = System.currentTimeMillis();
                    long count = metodoSerialCPU(conteudo, palavraAlvo);
                    long fim = System.currentTimeMillis();
                    resultados.add(new Resultado(arquivo, "SerialCPU", count, (fim - inicio)));
                }

                // --- Parallel CPU: testes com diferentes números de núcleos (Objetivo 3) ---
                int available = Runtime.getRuntime().availableProcessors();
                int[] configuracoesCores = new int[]{1, 4, Math.max(1, available)}; // ajuste conforme desejar

                for (int cores : configuracoesCores) {
                    System.out.println("   --> Testando ParallelCPU com " + cores + " núcleos...");
                    for (int i = 0; i < numExecucoes; i++) {
                        long inicio = System.currentTimeMillis();
                        long count = metodoParallelCPU_Ajustavel(conteudo, palavraAlvo, cores);
                        long fim = System.currentTimeMillis();
                        resultados.add(new Resultado(arquivo, "ParallelCPU_Cores_" + cores, count, (fim - inicio)));
                    }
                }

                // --- Parallel GPU (OpenCL) ---
                CLConfig clConfig = null;
                try {
                    clConfig = setupOpenCL();
                    for (int i = 0; i < numExecucoes; i++) {
                        long inicio = System.currentTimeMillis();
                        long count = metodoParallelGPU(clConfig, conteudo, palavraAlvo);
                        long fim = System.currentTimeMillis();
                        resultados.add(new Resultado(arquivo, "ParallelGPU", count, (fim - inicio)));
                    }
                } catch (Exception e) {
                    System.err.println("Aviso: OpenCL não disponível ou falha: " + e.getMessage());
                } finally {
                    if (clConfig != null) {
                        liberarOpenCL(clConfig);
                    }
                }

            } catch (IOException e) {
                System.err.println("Erro ao ler arquivo " + arquivo + ": " + e.getMessage());
            }
        }

        gerarCSV(resultados);
        exibirResumo(resultados);
    }

    // --- Métodos de Busca ---

    public static long metodoSerialCPU(String texto, String palavra) {
        long count = 0;
        // Tokenização simples por espaço
        String[] palavras = texto.split("\\s+");
        for (String w : palavras) {
            if (w.equalsIgnoreCase(palavra)) {
                count++;
            }
        }
        return count;
    }

    public static long metodoParallelCPU(String texto, String palavra) {
        // Uso de Streams paralelos (ForkJoinPool interno)
        return Arrays.stream(texto.split("\\s+"))
                .parallel()
                .filter(w -> w.equalsIgnoreCase(palavra))
                .count();
    }

    /**
     * Versão paralela na CPU que utiliza um ForkJoinPool customizado 
     * para investigar o impacto do número de núcleos. (Objetivo 3)
     */
    public static long metodoParallelCPU_Ajustavel(String texto, String palavra, int cores) {
        ForkJoinPool customPool = new ForkJoinPool(cores);

        try {
            return customPool.submit(() ->
                    Arrays.stream(texto.split("\\s+"))
                            .parallel()
                            .filter(w -> w.equalsIgnoreCase(palavra))
                            .count()
            ).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return 0;
        } finally {
            customPool.shutdown();
        }
    }

    public static long metodoParallelGPU(CLConfig config, String texto, String palavra) {
        int n = texto.length();
        int m = palavra.length();

        byte[] textBytes = texto.getBytes();
        byte[] patternBytes = palavra.getBytes();

        // Ponteiros
        Pointer srcTextPtr = Pointer.to(textBytes);
        Pointer srcPatternPtr = Pointer.to(patternBytes);
        int[] result = new int[1];
        result[0] = 0; // inicializa contador
        Pointer dstResultPtr = Pointer.to(result);

        // Alocação de memória na GPU
        cl_mem memText = clCreateBuffer(config.context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_char * n, srcTextPtr, null);
        cl_mem memPattern = clCreateBuffer(config.context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                Sizeof.cl_char * m, srcPatternPtr, null);
        cl_mem memOutput = clCreateBuffer(config.context, CL_MEM_READ_WRITE,
                Sizeof.cl_int, null, null);

        // Inicializa o buffer de saída com zero
        clEnqueueWriteBuffer(config.commandQueue, memOutput, CL_TRUE, 0,
                Sizeof.cl_int, dstResultPtr, 0, null, null);

        // Configurar argumentos do Kernel
        clSetKernelArg(config.kernel, 0, Sizeof.cl_mem, Pointer.to(memText));
        clSetKernelArg(config.kernel, 1, Sizeof.cl_int, Pointer.to(new int[]{n}));
        clSetKernelArg(config.kernel, 2, Sizeof.cl_mem, Pointer.to(memPattern));
        clSetKernelArg(config.kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{m}));
        clSetKernelArg(config.kernel, 4, Sizeof.cl_mem, Pointer.to(memOutput));

        // Definir tamanho do trabalho global
        long global_work_size[] = new long[]{n};

        // Executar
        clEnqueueNDRangeKernel(config.commandQueue, config.kernel, 1, null,
                global_work_size, null, 0, null, null);

        // Ler resultado
        clEnqueueReadBuffer(config.commandQueue, memOutput, CL_TRUE, 0,
                Sizeof.cl_int, dstResultPtr, 0, null, null);

        // Limpar memória da execução específica
        clReleaseMemObject(memText);
        clReleaseMemObject(memPattern);
        clReleaseMemObject(memOutput);

        return result[0];
    }

    // --- Helpers e Estruturas ---

    static class Resultado {
        String arquivo, metodo;
        long ocorrencias, tempoMs;

        public Resultado(String a, String m, long o, long t) {
            this.arquivo = a; this.metodo = m; this.ocorrencias = o; this.tempoMs = t;
        }
    }

    static void gerarCSV(List<Resultado> resultados) {
        try (PrintWriter pw = new PrintWriter(new File("resultados_busca.csv"))) {
            pw.println("Arquivo,Metodo,Ocorrencias,Tempo_ms");
            for (Resultado r : resultados) {
                // Escape simples de vírgulas no nome do arquivo (caso)
                String arquivoEsc = r.arquivo.replace(",", "_");
                pw.printf("%s,%s,%d,%d%n", arquivoEsc, r.metodo, r.ocorrencias, r.tempoMs);
            }
            System.out.println("\nCSV gerado: resultados_busca.csv");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    static void exibirResumo(List<Resultado> resultados) {
        // Exibe apenas a primeira execução de cada tipo para o console (exemplo)
        Map<String, Boolean> exibido = new HashMap<>();
        for (Resultado r : resultados) {
            String key = r.arquivo + "|" + r.metodo;
            if (!exibido.containsKey(key)) {
                System.out.printf("%s - %s: %d ocorrências em %d ms%n",
                        r.arquivo, r.metodo, r.ocorrencias, r.tempoMs);
                exibido.put(key, true);
            }
        }
    }

    // --- Boilerplate OpenCL ---
    static class CLConfig {
        cl_context context;
        cl_command_queue commandQueue;
        cl_kernel kernel;
        cl_program program;
    }

    static CLConfig setupOpenCL() {
        CLConfig c = new CLConfig();
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_GPU; // Mude para CL_DEVICE_TYPE_CPU se não tiver GPU dedicada
        final int deviceIndex = 0;

        CL.setExceptionsEnabled(true);
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];
        if (numPlatforms == 0) throw new RuntimeException("Nenhuma plataforma OpenCL encontrada.");
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];
        if (numDevices == 0) throw new RuntimeException("Nenhum dispositivo OpenCL do tipo requisitado encontrado.");
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];

        c.context = clCreateContext(contextProperties, 1, new cl_device_id[]{device}, null, null, null);

        c.commandQueue = clCreateCommandQueueWithProperties(c.context, device, new cl_queue_properties(), null);

        c.program = clCreateProgramWithSource(c.context, 1, new String[]{programSource}, null, null);
        clBuildProgram(c.program, 0, null, null, null, null);
        c.kernel = clCreateKernel(c.program, "countWords", null);

        return c;
    }

    static void liberarOpenCL(CLConfig c) {
        try {
            if (c.kernel != null) clReleaseKernel(c.kernel);
        } catch (Exception ignored) {}
        try {
            if (c.program != null) clReleaseProgram(c.program);
        } catch (Exception ignored) {}
        try {
            if (c.commandQueue != null) clReleaseCommandQueue(c.commandQueue);
        } catch (Exception ignored) {}
        try {
            if (c.context != null) clReleaseContext(c.context);
        } catch (Exception ignored) {}
    }
}
