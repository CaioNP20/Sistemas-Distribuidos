def ler_arquivo(nome):
    with open(nome, "r", encoding="utf-8") as f:
        return f.read()

arquivo1 = "server_9101.txt"
arquivo2 = "server_9102.txt"
arquivo3 = "server_9103.txt"

conteudo1 = ler_arquivo(arquivo1)
conteudo2 = ler_arquivo(arquivo2)
conteudo3 = ler_arquivo(arquivo3)

if conteudo1 == conteudo2 == conteudo3:
    print("Os três arquivos possuem o MESMO conteúdo.")
else:
    print("Os arquivos são DIFERENTES entre si.")
