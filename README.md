

***

# Análise de Desempenho: Algoritmos Paralelos CPU e GPU

## Título: Análise comparativa de algoritmos com uso de paralelismo

O objetivo deste projeto é fornecer uma análise detalhada do desempenho de algoritmos de busca (contagem de palavras) em diferentes ambientes de processamento: Serial, Paralelo (CPU) e Paralelo (GPU), utilizando a linguagem Java. O foco é entender como a eficiência computacional se altera ao variar o algoritmo e o volume dos dados de entrada.

---

## 📄 Resumo

Este trabalho propõe uma análise detalhada do desempenho de diferentes algoritmos de busca em ambientes seriais e paralelos, utilizando a linguagem de programação Java. Neste estudo, serão abordados três algoritmos: **Serial**, **Paralelo em CPU** e **Paralelo em GPU (via OpenCL)**. Os resultados são registrados em arquivos CSV e visualizados através de gráficos para facilitar a análise comparativa.

## 📝 Introdução

A busca por eficiência computacional é essencial em diversas aplicações. A abordagem do projeto envolve a criação de um programa em Java com três métodos distintos para a contagem de ocorrências de uma palavra em um texto:

*   **Metodo SerialCPU:** Utiliza um *loop* simples para iterar sobre cada palavra do texto e contar as ocorrências.
*   **Metodo ParallelCPU:** Utiliza um *pool* de *threads* (ajustável) para dividir o texto em partes e realizar a contagem em paralelo.
*   **Metodo ParallelGPU:** Utiliza a biblioteca OpenCL para processar o texto em paralelo diretamente na GPU, visando contagens de palavras de forma eficiente.

A saída de cada execução registra a contagem da palavra e o tempo de execução.

## 🛠️ Metodologia

O trabalho seguiu uma metodologia clara baseada em quatro etapas principais:

1.  **Implementação de Algoritmos:** Criação dos algoritmos de busca sequenciais e paralelos em Java (`BuscaPalavraComparativa`).
2.  **Framework de Teste:** Desenvolvimento de um *framework* para executar e registrar os tempos de execução, variando o tamanho e a natureza dos conjuntos de dados de entrada (`MobyDick-217452.txt`, `DonQuixote-388208.txt`, `Dracula-165307.txt`).
3.  **Execução em Ambientes Variados e Amostragem:** Foram realizadas **pelo menos 3 amostras de cada execução** (Serial, Parallel CPU, Parallel GPU). Adicionalmente, foi investigado o comportamento dos algoritmos Paralelos na CPU ajustando o número de núcleos disponíveis.
4.  **Registro de Dados e Análise Estatística:** O *framework* registra os dados em `resultados_busca.csv`. A **Análise Estatística** utiliza o código `AnaliseEGerarGrafico` para agregar as métricas, calcular as médias de tempo, e gerar os gráficos de desempenho.

## ⚙️ Dependências do Projeto

Para a execução e análise corretas do projeto, são necessárias as seguintes bibliotecas Java, que devem ser configuradas (idealmente via Maven ou adicionadas ao *classpath*):

| Dependência | Versão (Exemplo) | Propósito | Configuração Necessária |
| :--- | :--- | :--- | :--- |
| **JOCL** | `2.0.4` | Permite o uso do OpenCL (Caminho para o *kernel* OpenCL) para o **Metodo ParallelGPU**. | A biblioteca JOCL (`jocl-2.0.4`) e suas dependências nativas devem ser **explicadas e indicadas para o ambiente de correção**. |
| **JFreeChart** | `1.5.4` | Utilizada pela classe `AnaliseEGerarGrafico` para a **geração automática dos gráficos** (em formato PNG). | Necessário para a geração visual dos resultados (Objetivo 5). |
| **Apache Commons CSV** | `1.10.0` | Usada opcionalmente para manipulação de CSV (apesar do código usar `PrintWriter`, a dependência está listada na configuração do projeto). | Auxilia no processamento e agregação de dados. |

### Configuração OpenCL (JOCL)

Para que o `metodoParallelGPU` funcione, o ambiente deve suportar OpenCL e a biblioteca JOCL deve estar corretamente instalada e acessível. O código OpenCL (`programSource`) está embutido na classe `BuscaPalavraComparativa`.

## 🚀 Como Executar

O projeto envolve as seguintes classes principais:

1.  **`BuscaPalavraComparativa.java`:** Responsável pela execução dos três métodos (Serial, Parallel CPU com ajuste de núcleos, e Parallel GPU) e pela geração do arquivo de resultados brutos: **`resultados_busca.csv`**.
2.  **`AnaliseEGerarGrafico.java`:** Responsável por ler o `resultados_busca.csv`, calcular as médias de tempo, gravar as médias em **`medias_aggregadas.csv`**, e gerar o gráfico de barras **`medias_desempenho.png`**.

**Passos:**

1.  Garanta que os arquivos de texto (`MobyDick-217452.txt`, etc.) e as dependências (JOCL, JFreeChart) estejam no diretório do projeto.
2.  Execute a classe `BuscaPalavraComparativa.java` para coletar os dados.
3.  Execute a classe `AnaliseEGerarGrafico.java` para processar os dados e gerar o gráfico PNG.


## 📊 Resultados e Discussão

Esta seção apresenta os resultados obtidos nos testes de desempenho comparativos realizados com os três métodos de busca (Serial, Parallel CPU, Parallel GPU) em diferentes volumes de dados.

O processo de coleta de dados envolveu a execução de **pelo menos 3 amostras** para cada cenário, cujas médias de tempo foram calculadas pela classe `AnaliseEGerarGrafico`, resultando na tabela `medias_aggregadas.csv` e no gráfico de desempenho `medias_desempenho.png`.

### Demonstração do Gráfico

O gráfico , gerado pela biblioteca JFreeChart, exibe o tempo médio de execução em milissegundos (ms) para cada combinação de arquivo e método de processamento.

### Discussão sobre a Variação do Desempenho ao Mudar o Tamanho do Arquivo

Os testes foram realizados em três arquivos de tamanhos variados (`MobyDick`, `DonQuixote` e `Dracula`).

1.  **Impacto do Volume:** Observa-se que o tempo de execução é diretamente proporcional ao tamanho do arquivo para todos os métodos. O arquivo com maior tempo de processamento em quase todas as variantes é o `MobyDick`, que demonstra a barra de tempo mais alta (cerca de **130 ms** no pior cenário).
2.  **Escalabilidade do Serial:** Os métodos seriais e as variantes com baixo paralelismo (Parallel CPU com 1 núcleo) tendem a apresentar os maiores aumentos de tempo à medida que o tamanho do arquivo cresce, conforme visível na comparação dos cenários de `Dracula` vs. `MobyDick`.

### Discussão sobre o *Speedup* Obtido com Paralelismo (CPU e GPU)

O *speedup* (ganho de velocidade) obtido com o paralelismo é evidente ao comparar os resultados de SerialCPU com ParallelCPU e ParallelGPU.

*   **Paralelismo em CPU (*Speedup* Moderado):** Em cenários como o de `DonQuixote`, o método SerialCPU opera em aproximadamente **95 ms**, enquanto as variantes ParallelCPU mais rápidas (variando o número de *cores*) conseguem reduzir esse tempo para a faixa de **65 ms a 75 ms**. Isso demonstra que o uso do `ForkJoinPool` do Java oferece um ganho de desempenho significativo ao dividir a carga de trabalho.
*   **Paralelismo em GPU (*Speedup* Elevado):** A maior diferença de desempenho é notada com o uso do **ParallelGPU (OpenCL)**.
    *   Para os arquivos menores (`Dracula` e `DonQuixote`), o tempo de execução do ParallelGPU é drasticamente inferior, atingindo cerca de **7 ms a 8 ms**, representando o cenário mais eficiente.
    *   Mesmo no maior arquivo (`MobyDick`), onde a sobrecarga de transferência de dados (Host para Device) é maior, o ParallelGPU ainda se posiciona entre as opções mais rápidas, demonstrando a eficácia do processamento OpenCL.

### Análise do Impacto de Ajustar o Número de Núcleos de Processamento para o `ParallelCPU` (Objetivo 3)

Conforme a metodologia, o `metodoParallelCPU_Ajustavel` foi utilizado para investigar o impacto da configuração do *pool* de *threads*, testando 1, 4 e o número máximo de núcleos disponíveis.

A análise das barras do ParallelCPU (que correspondem a múltiplas barras por arquivo além da Serial e GPU) mostra que:

*   **Vantagem do Aumento de Núcleos:** Em geral, o tempo de execução diminui quando se aumenta o número de núcleos de 1 (simulação quase serial) para 4 ou para o total disponível. Esta otimização é crucial para obter o *speedup* prometido pelo algoritmo paralelo.
*   **Ponto de Saturação:** Embora o aumento de núcleos melhore o desempenho, o ganho marginal tende a diminuir após um certo ponto (Lei de Amdahl). É importante notar, contudo, que mesmo a melhor configuração de ParallelCPU (múltiplos *cores*) não consegue igualar a performance do ParallelGPU nos dados testados.

## ✅ Conclusão

O trabalho alcançou seu **objetivo principal** de fornecer uma análise detalhada do desempenho comparativo entre algoritmos de busca (contagem de palavras) em ambientes seriais e paralelos (CPU e GPU), utilizando a linguagem Java.

A execução dos métodos (`metodoSerialCPU`, `metodoParallelCPU_Ajustavel` e `metodoParallelGPU`) em diferentes volumes de dados, e a coleta de **pelo menos 3 amostras de cada execução**, permitiram a realização da **Análise Estatística** necessária. Esta análise foi concretizada pela classe `AnaliseEGerarGrafico` que leu o `resultados_busca.csv` e calculou as médias de tempo, gravando-as no `medias_aggregadas.csv` e gerando o gráfico `medias_desempenho.png`.

**As principais conclusões extraídas são:**

1.  **Superioridade do Paralelismo em GPU:** O `ParallelGPU`, que utiliza OpenCL e a biblioteca JOCL, demonstrou ser o método mais eficiente, registrando consistentemente os **menores tempos médios de execução** conforme ilustrado no gráfico. Este resultado confirma que o processamento em GPU é o mais adequado para obter um *speedup* significativo em tarefas de busca e contagem com alto grau de paralelismo de dados.
2.  **Ganhos com Paralelismo em CPU:** O `ParallelCPU` (Streams paralelos), especialmente quando investigado com a customização do número de núcleos de processamento, confirmou a obtenção de ganhos de desempenho notáveis em comparação com o método `SerialCPU`. A variação de *cores* (1, 4, ou máximo) forneceu *insights* sobre a importância da otimização do *pool* de *threads* para melhor escalabilidade, embora não tenha atingido a mesma performance do ParallelGPU.
3.  **Impacto do Volume de Dados:** Observou-se que o tempo de execução aumenta de maneira previsível para todos os métodos à medida que o tamanho do arquivo cresce (como no processamento do Moby Dick em comparação com Don Quixote ou Dracula).

Em resumo, os resultados fornecem *insights* valiosos sobre quais meios de processamento são mais adequados para diferentes volumes de massa e como o desempenho é afetado por fatores como o tamanho do conjunto de dados e o meio de processamento. Este trabalho contribui para o avanço do conhecimento em computação concorrente e paralela.


## 🔗 Anexos / Link do Projeto no GITHUB

*   **Códigos das Implementações:** `BuscaPalavraComparativa.java`, `AnaliseEGerarGrafico.java`, e o *kernel* OpenCL.
*   **Link do Projeto no GITHUB:**
    `[INSERIR O LINK DO REPOSITÓRIO AQUI]`

***
**NOTA IMPORTANTE PARA A CORREÇÃO:** Lembre-se de anexar um arquivo PDF deste README juntamente com o link do GITHUB. Certifique-se de que todas as bibliotecas, especialmente JOCL, estejam devidamente configuradas no ambiente de execução ou que haja instruções claras sobre como colocá-las.
