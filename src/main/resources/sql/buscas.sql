 -- Dados auxiliares criar a tabela Crescimento
SELECT ai.quantdomiciliosacesso, ai.quantdomicilios, ai.codestado, ai.codtempo
FROM acessointernet ai
ORDER BY codestado, codtempo

--Questão 1
SELECT e.regiao ,SUM(ai.quantdomiciliosacesso), SUM(ai.quantdomicilios), SUM(ai.quantescolasacesso), SUM(ai.quantescolas)
FROM acessointernet ai
JOIN tempo tp ON (ai.codTempo = tp.codTempo)
JOIN estado e ON (ai.codEstado = e.codEstado)
WHERE tp.ano = 2018
GROUP BY e.regiao

--Questão 2
SELECT e.nome, t.ano, ai.pib, c.taxa 
FROM acessointernet ai
JOIN tempo t ON (ai.codtempo=t.codtempo)
JOIN crescimento c ON (ai.codtempo=t.codtempo)
JOIN estado e ON (ai.codestado = e.codestado)

--Questão 3
	-- Filtro reasidências
	-- 3 estados com menor quantidade de domicios com acesso
SELECT ai.codestado, e.nome
FROM acessointernet ai
JOIN tempo t ON (ai.codtempo=t.codtempo)
JOIN Estado e ON (ai.codestado=e.codestado)
WHERE t.ano = 2018
ORDER BY ai.quantdomiciliosacesso asc
LIMIT 3
	-- buscar as taxas de crescimento de domicilios com acesso dos ultimos 5 anos
	-- para os estados com menor quantidade de domicilios com acesso
	-- ? = resultado da busca anterior
SELECT e.nome, c.taxa, t.ano
FROM crescimento c
JOIN tempo t ON (c.codtempo=t.codtempo)
JOIN estado e ON (c.codestado=e.codestado)
JOIN acessointernet ai ON (c.codtempo=ai.codtempo and c.codestado=ai.codestado)
WHERE t.ano IN (2018,2017,2016,2015,2014)
AND c.codestado IN (?)
ORDER BY e.nome, t.ano
	-- codigo e quantidade de dom. com acesso dos 3 estados com maior quantidade
	-- de domicilios com acesso
SELECT ai.codestado, ai.quantdomiciliosacesso 
FROM acessointernet ai
JOIN tempo t ON (ai.coptempo=t.codtempo) 
WHERE t.ano = 2018
ORDER BY ai.quantdomiciliosacesso DESC
LIMIT 3

--Questão 4
SELECT ai.codestado, ai.quantdomiciliosacesso, ai.quantescolasacesso, ai.pib 
FROM acessointernet ai
ORDER BY ai.codtempo
