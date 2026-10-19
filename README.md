# Agência de Voos — AED III, Fase III

Aplicação Java Swing para gerenciar clientes, voos e check-in de passageiros,
com dados e índices em arquivos binários. As funcionalidades de uso do sistema
estão disponíveis na interface gráfica; o usuário não precisa de console.

## Funcionalidades

- CRUD de clientes e voos, com PKs estáveis, exclusão lógica e Hash Extensível
  persistente para localizar os registros por ID.
- Relacionamento N:N: um cliente pode participar de vários voos e um voo pode
  ter vários clientes. A associação tem chave composta **(idCliente, idVoo)**,
  sem ID artificial, e não pode ser cadastrada em duplicidade.
- Inclusão e remoção de vínculos e consultas completas nos dois sentidos,
  disponíveis em **Check-in**. Na interface, o vínculo cliente–voo é apresentado
  como check-in; o modelo técnico permanece N:N.
- Duas Árvores B+ persistidas: `idCliente → IDs dos voos` e
  `idVoo → IDs dos clientes`. As consultas usam esses índices e recuperam
  as entidades pelo Hash, sem varrer o intermediário a cada consulta.
- Integridade referencial no serviço: somente clientes e voos existentes
  podem ser associados. Excluir uma entidade com vínculos é bloqueado;
  primeiro remova suas associações. Alterar nome, código ou demais dados
  não muda os IDs nem perde os vínculos.
- Ordenação externa dos voos por data e horário, por intercalação de duas vias,
  com blocos de até três registros e arquivos temporários em disco.
- Migração automática dos vínculos válidos da Fase II, com backup dos arquivos
  originais e marcador para não reimportar associações já removidas.

## Como executar

Requer JDK 21, sem bibliotecas externas. Abra a raiz do projeto, onde estão
`src` e `dados`, e compile antes de executar. Em PowerShell:

```powershell
$fontesAgencia = @(Get-ChildItem -LiteralPath .\src -Filter *.java -File | ForEach-Object { $_.FullName })
javac -encoding UTF-8 --release 21 -d bin $fontesAgencia
if ($LASTEXITCODE -ne 0) { throw "A compilação falhou." }
java -cp bin Principal
```

Se o JDK não estiver no PATH, use os caminhos da instalação. Por exemplo:

```powershell
$fontesAgencia = @(Get-ChildItem -LiteralPath .\src -Filter *.java -File | ForEach-Object { $_.FullName })
& 'C:\Program Files\Java\jdk-21.0.12\bin\javac.exe' -encoding UTF-8 --release 21 -d bin $fontesAgencia
if ($LASTEXITCODE -ne 0) { throw "A compilação falhou." }
& 'C:\Program Files\Java\jdk-21.0.12\bin\java.exe' -cp bin Principal
```

No VS Code, abra a pasta do projeto, aguarde a análise Java, selecione a
configuração **Agência de Voos** em Executar e Depurar e pressione F5.
A aplicação usa Swing e não precisa receber comandos pelo console.

Se o aviso **Build failed** persistir apesar da compilação
correta no terminal, execute **Java: Clean Java Language Server Workspace** na
paleta de comandos, confirme a limpeza/reinício e aguarde a análise terminar.
Não use **Always Continue** para esconder erros de compilação.

`bin` não é versionada: recompile após clonar ou limpar os `.class`.
`ClassNotFoundException: Principal` indica que a classe não foi gerada ou que
o classpath não aponta para `bin`. Mantenha uma única instância da agência
aberta por base de dados; o intermediário N:N possui bloqueio entre processos.

## Uso pela interface

1. Em **Gerenciar Clientes**, cadastre ou consulte os clientes. A seleção de
   uma linha preenche o formulário e mostra os voos associados.
2. Em **Gerenciar Voos**, salve os voos e consulte-os pelo ID ou código.
   O cadastro do voo não substitui nem remove associações existentes. Use
   **Ordenar voos** para ver a lista por data e horário no painel à direita;
   o arquivo ordenado também é gerado em disco.
3. Abra **Check-in**. Selecione um passageiro e um voo e clique em
   **Fazer check-in**. O resultado aparece na própria tela.
4. Faça o check-in de outro passageiro no mesmo voo e do primeiro passageiro
   em outro voo. Use **Voos do passageiro** e **Passageiros do voo** para conferir
   todos os registros.
5. Tente repetir o mesmo par: a interface informa que ele já existe.
6. Selecione o passageiro e o voo e use **Cancelar check-in**. As duas consultas
   são atualizadas; os outros check-ins e os cadastros continuam preservados.
7. Para excluir um cliente ou voo, cancele antes todos os seus check-ins.
   Feche e reabra o programa para conferir a persistência.

A interface usa uma paleta consistente em azul-marinho, azul e verde-petróleo,
com texto de alto contraste nos botões, formulários e tabelas. Os nomes técnicos
do relacionamento ficam na documentação e no código, não na navegação.

## Arquivos e implementação

| Arquivo | Finalidade |
| --- | --- |
| `dados/clientes/clientes.db` | Registros dos clientes |
| `dados/voos/voos.db` | Registros dos voos |
| `*.db.hash.dir.db` e `*.db.hash.buckets.db` | Hash Extensível das PKs |
| `dados/associacoes/clientes_voos.db` | Intermediário binário N:N |
| `dados/associacoes/clientes_voos.cliente.bplus.db` | B+ de cliente para voos |
| `dados/associacoes/clientes_voos.voo.bplus.db` | B+ de voo para clientes |
| `dados/associacoes/migracao_fase_iii_v1.done` | Marca a migração concluída |
| `dados/associacoes/backup_fase_ii/` | Originais anteriores à migração |
| `dados/voos/voos_ordenados_por_data_hora.db` | Resultado da ordenação externa |

O intermediário tem cabeçalho com identificação e versão. Cada registro ocupa
13 bytes: estado/lápide (1), ID do cliente (4), ID do voo (4) e CRC32 dos IDs
(4). Um mapa ordenado em memória relaciona a chave composta ao endereço do
registro para controlar duplicidade e localizar a exclusão. Ele é reconstruído
na abertura e não substitui as duas B+ utilizadas nas consultas.

As B+ são estruturas reais com nós internos, separadores e folhas encadeadas,
carregadas em memória e serializadas em binário. A implementação existente
reconstrói a árvore completa a cada manutenção. Na abertura da Fase III, ambas
são regeneradas a partir do intermediário, com arquivo temporário e substituição,
para não confiar em índices antigos ou corrompidos. Isso aumenta o custo de
inicialização/manutenção, mas as consultas regulares percorrem os índices.

`AgenciaService` valida as referências, coordena as operações e compartilha um
único `ClienteDAO` com as telas. A exclusão direta por `ClienteDAO` também
confere o intermediário e bloqueia clientes associados. `ClienteVooDAO` é a
camada interna de armazenamento; a validação da existência das entidades
pertence ao serviço e não deve ser contornada chamando o armazenamento direto.

## Migração, segurança e limites

- Na primeira abertura, os arquivos binários de clientes, voos e o intermediário
  legado, se existir, são copiados para `backup_fase_ii` antes da conversão.
  Vínculos válidos da FK antiga e do arquivo legado são importados sem duplicar.
  Referências antigas inválidas são informadas pela interface e preservadas
  nos originais do backup, não importadas para o N:N.
- Após importar, a FK antiga é neutralizada. O campo permanece no formato de
  `Voo` para ler a Fase II, mas não representa mais o relacionamento ativo.
  Novos vínculos usam o intermediário. Não apague o marcador de migração.
- O intermediário N:N e suas duas B+ são sincronizados em disco. Se uma gravação falhar,
  o DAO tenta recuperar os índices; se não conseguir, recusa novas operações
  até a reabertura. Uma operação que informou falha pode já ter gravado o vínculo:
  consulte a tela antes de repeti-la. Não há transação multiarquivo nem rollback
  geral de dados, Hash, B+ e migração.
- Corrupção dos dados ativos gera erro explícito; não existe recuperação
  automática de toda forma de corrupção. Faça cópias da pasta `dados` com o
  programa fechado. As bases locais e os backups não são versionados.
- A ordenação externa gera e intercala runs em disco sem carregar todos os
  voos na memória. O fluxo normal remove temporários; falhas de I/O podem
  deixar arquivos temporários para conferência.

## Documentação

O [PDF da Fase II](output/pdf/Fase%20II%20-%20Decisoes%20de%20Projeto.pdf)
é mantido como documento histórico daquela etapa; ele não descreve o novo N:N.

O [vídeo demonstrativo da Fase III](video/TP3_AED.mp4) está neste repositório.
Para assistir, abra o link, use a opção de download do GitHub e reproduza o
arquivo MP4 no seu computador. A página do arquivo pode não exibir uma prévia
do vídeo.
