import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.text.Normalizer;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
    static final String[] codStates = {
            "11", "12", "13", "14", "15", "16", "17", "21", "22", "23", "24", "25", "26", "27", "28", "29", "31", "32", "33", "35", "41", "42", "43", "50", "51", "52", "53"
    };
    static final List<String> nameStates = Arrays.asList(
            "Rondônia", "Acre", "Amazonas", "Roraima", "Pará", "Amapá", "Tocantins", "Maranhão", "Piauí",
            "Ceará", "Rio Grande do Norte", "Paraíba", "Pernambuco", "Alagoas", "Sergipe", "Bahia",
            "Minas Gerais", "Espírito Santo", "Rio de Janeiro", "São Paulo", "Paraná", "Santa Catarina",
            "Rio Grande do Sul", "Mato Grosso do Sul", "Mato Grosso", "Goiás", "Distrito Federal");
    static final String[] ufs = {
            "ro", "ac", "am", "rr", "pa", "ap", "to", "ma", "pi", "ce", "rn", "pb", "pe", "al",
            "se", "ba", "mg", "es", "rj", "sp", "pr", "sc", "rs", "ms", "mt", "go", "df"
    };
    static final String yourPath = "/";
    static final String path = yourPath +"etlconexoes/dados/";
    static final String pathZips = yourPath + "etlconexoes/dados/zips/";
    static final String pathUnzips = yourPath +"etlconexoes/dados/unzips/";
    static final String pathEtls = yourPath + "etlconexoes/dados/etlresult/";
    static final Integer PNAD_VALIDATION = 1;
    static final Integer MICRO_DADOS_VALIDATION = 2;

    public static void createDirectories() {
        createDirectory(path);

        createDirectory(pathZips);

        createDirectory(pathUnzips);

        createDirectory(pathEtls);

        List<Integer> yearsSearch = new ArrayList<>(List.of(2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018));
        for (Integer yearActual : yearsSearch) {
            createDirectory(pathZips + yearActual +  "/");
            createDirectory(pathUnzips + yearActual +  "/");
            createDirectory(pathEtls + yearActual +  "/");
        }
    }

    public static void createDirectory(String path) {
        File directory = new File(path);
        if (directory.exists()) {
            System.out.println("Path "+ directory.getPath() +" already exists");
            return;
        }
        System.out.println("Creating directory " + directory.getPath());
        directory.mkdirs();
    }

    public static void main(String[] args) {
        List<Integer> yearsSearch = new ArrayList<>(List.of(2011, 2012, 2013, 2014, 2015)); //2011, 2012, 2013, 2014, 2015
        StringBuilder pathZipsActual;
        StringBuilder pathUnzipActual;
        //StringBuilder pathEtlActual;

        createDirectories();

        String inputFile = "~~/Downloads/teste.ris";
        String outputFile = "~==~/Downloads/xablau.csv";

        try {
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            FileWriter fw = new FileWriter(outputFile);

            String line;
            StringBuilder csvData = new StringBuilder();
            boolean insideEntry = false;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("TY")) {
                    insideEntry = true;
                    csvData.setLength(0);
                } else if ((line.trim().isEmpty() || line.startsWith("ER")) && insideEntry) {
                    insideEntry = false;
                    fw.write(csvData.toString() + "\n");
                } else if (insideEntry) {
                    String[] parts = line.split("  - ", 2);
                    if (parts.length == 2) {
                        String fieldName = parts[0].trim();
                        String fieldValue = parts[1].trim();
                        csvData.append(fieldValue).append(";");
                    }
                }
            }

            br.close();
            fw.close();

            System.out.println("Conversão concluída com sucesso!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (true)
            return;
        HashMap<String, EtlData> etlDataMap = collectDataMicroDados();

        etlDataMap = collectDataPIB(etlDataMap);

        StringBuilder insertAcessos = new StringBuilder();
        insertAcessos.append("INSERT INTO public.acessointernet(codtempo, codestado, quantdomicilios, quantdomiciliosacesso, quantescolas, quantescolasacesso, pib) \n VALUES");
        for (Integer yearActual : yearsSearch) {
            for (int i = 0; i < codStates.length; i++) {
                pathZipsActual = new StringBuilder(pathZips);
                pathUnzipActual = new StringBuilder(pathUnzips);
                String ftpLink;
                if (yearActual == 2015)
                    ftpLink = "https://ftp.ibge.gov.br/Trabalho_e_Rendimento/Pesquisa_Nacional_por_Amostra_de_Domicilios_anual/" + yearActual + "/Volume_Brasil/Unidades_da_Federacao/" + removeAccents(nameStates.get(i)).replaceAll(" ", "_") + "_xls.zip";
                else
                    ftpLink = "https://ftp.ibge.gov.br/Trabalho_e_Rendimento/Pesquisa_Nacional_por_Amostra_de_Domicilios_anual/" + yearActual + "/Volume_Brasil/Unidades_da_Federacao/brasil_tc_ufs_" + codStates[i] + ufs[i] + "_xls.zip";

                System.out.println("\nurl Download PNAD zip: " + ftpLink);
                File directoryYear = new File(pathZipsActual.append(yearActual).append("/").toString());
                System.out.println(pathZipsActual + "\n");
                if (!directoryYear.exists()) {
                    directoryYear.mkdirs();
                    //continue;
                }

                String localFilePath = pathZipsActual.append(ufs[i]).append(".zip").toString();

                try {
                    // Download the file via ftpLink
                    downloadZip(localFilePath, ftpLink, ufs[i]);
                    System.out.println("File downloaded successfully.");

                    // Unzip the downloaded file
                    unzipFile(localFilePath, pathUnzipActual.append(yearActual).append("/").toString(), ufs[i], "xls");
                    System.out.println("File unzipped successfully.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //ETL PROCESS (2011-2015)
            String xlsPath = pathUnzips + yearActual + "/";
            StringBuilder xlsPathActual;
            String searchValue = "Total,Com acesso à Internet,";
            List<EtlData> etls = new ArrayList<>();
            for (int i = 0; i < codStates.length; i++) {
                xlsPathActual = new StringBuilder(xlsPath);
                xlsPathActual.append(ufs[i]).append(".xls");
                try (FileInputStream fis = new FileInputStream(xlsPathActual.toString());
                     Workbook workbook = new HSSFWorkbook(fis)) {
                    Sheet sheet = workbook.getSheetAt(0); // Cause the values is in the first sheet
                    String key = ufs[i].toUpperCase() + "-" + yearActual;
                    EtlData obj = etlDataMap.get(key);
                    if (obj == null)
                        System.out.println("this key is null -> " + key);
                    for (Row row : sheet) {
                        Cell cell = row.getCell(0); // Cause the value in the first column
                        String cellValue;
                        if (cell == null || cell.getCellTypeEnum() != CellType.STRING || strEmpty(cell.getStringCellValue()) || !searchValue.contains(cellValue = cell.getStringCellValue().trim())) {
                            continue;
                        }

                        Cell nextCell = row.getCell(1);

                        if (nextCell == null) {
                            continue;
                        }
                        switch (cellValue) {
                            case "Total" -> obj.setQuantDomicilios(nextCell.getNumericCellValue());
                            case "Com acesso à Internet" -> obj.setQuantDomiciliosAcesso(nextCell.getNumericCellValue());
                        }
                    }
                    etlDataMap.put(key, obj);
                    etls.add(obj);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            createEtlAcessos(new StringBuilder(pathEtls), yearActual, etls, insertAcessos);
        }
        yearsSearch = new ArrayList<>(List.of(2016, 2017, 2018));//
        //DOWNLOAD 2016 - 2018
        for (Integer yearActual : yearsSearch) {
            pathZipsActual = new StringBuilder(pathZips);
            pathUnzipActual = new StringBuilder(pathUnzips);
            String extension;
            String ftpLink;
            if (yearActual == 2018) {
                extension = "xlsx";
                ftpLink = "https://ftp.ibge.gov.br/Trabalho_e_Rendimento/Pesquisa_Nacional_por_Amostra_de_Domicilios_continua/Anual/Acesso_Internet_Televisao_e_Posse_Telefone_Movel_2018/xls/PNAD_Continua_2018_TIC_tabelas_domicilios_xls.zip";
            }
            else{
                extension = "xls";
                ftpLink = "https://ftp.ibge.gov.br/Trabalho_e_Rendimento/Pesquisa_Nacional_por_Amostra_de_Domicilios_continua/Anual/Acesso_Internet_Televisao_e_Posse_Telefone_Movel_" + yearActual + "/PNAD_Continua_" + yearActual + "_TIC_tabelas_domicilios_xls.zip";
            }
            System.out.println("\nurl Download zip: " + ftpLink);
            File directoryYear = new File(pathZipsActual.append(yearActual).append("/").toString());
            System.out.println(pathZipsActual + "\n");
            if (!directoryYear.exists()) {
                directoryYear.mkdirs();
                //continue;
            }

            String localFilePath = pathZipsActual.append(yearActual).append(".zip").toString();
            try {
                // Download the file via ftpLink
                downloadZip(localFilePath, ftpLink, yearActual.toString());
                System.out.println("File downloaded successfully.");

                // Unzip the downloaded file
                unzipFile(localFilePath, pathUnzipActual.append(yearActual).append("/").toString(), yearActual.toString(), extension);
                System.out.println("File unzipped successfully.");

            } catch (IOException e) {
                e.printStackTrace();
            }

            //ETL PROCESS (2016-2018)
            String xlsPath = pathUnzips + yearActual + "/";
            String searchValue = " ,Rondônia ,Acre ,Amazonas ,Roraima ,Pará ,Amapá ,Tocantins ,Maranhão ,Piauí" +
                    ",Ceará ,RioGrandedoNorte ,Paraíba ,Pernambuco ,Alagoas ,Sergipe ,Bahia" +
                    ",MinasGerais ,EspíritoSanto ,RiodeJaneiro ,SãoPaulo ,Paraná ,SantaCatarina" +
                    ",RioGrandedoSul ,MatoGrossodoSul ,MatoGrosso ,Goiás ,DistritoFederal ,";
            List<EtlData> etls = new ArrayList<>();
            StringBuilder xlsPathActual = new StringBuilder(xlsPath);
            xlsPathActual.append(yearActual).append(yearActual == 2018 ? ".xlsx" : ".xls");
            if (yearActual != 2018) {
                try (FileInputStream fis = new FileInputStream(xlsPathActual.toString());
                     Workbook workbook = new HSSFWorkbook(fis)) {
                    etls = fillEtls(workbook, yearActual, searchValue, etlDataMap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                try (FileInputStream fis = new FileInputStream(xlsPathActual.toString());
                     Workbook workbook = new XSSFWorkbook(fis)) {
                    etls = fillEtls(workbook, yearActual, searchValue, etlDataMap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            createEtlAcessos(new StringBuilder(pathEtls), yearActual, etls, insertAcessos);
        }
        System.out.println("-------------------- INSERT ACESSOSINTERNET ------------------------");
        System.out.println(insertAcessos);

        StringBuilder insertCrescimento = new StringBuilder();
        insertCrescimento.append("INSERT INTO public.crescimento(taxa, codestado, codtempo) VALUES\n");
        for (EtlData etlData : etlDataMap.values()) {
            if (equals(etlData.getNuAnoCenso(), "2011")) {
                insertCrescimento.append("(" + 0.00 +  ", " + etlData.getCodState() + ", " + etlData.getCodTime() + "),\n");
                continue;
            }

            String uf = etlData.getUf();
            String year = etlData.getNuAnoCenso();
            String previousKey = uf + "-" + (Integer.parseInt(year)-1);
            Double previousQuantDomiciliosAcesso = etlDataMap.get(previousKey).getQuantDomiciliosAcesso();
            Double rate = ((etlData.getQuantDomiciliosAcesso() - previousQuantDomiciliosAcesso) / previousQuantDomiciliosAcesso) * 100.00;
            insertCrescimento.append("(" + rate +  ", " + etlData.getCodState() + ", " + etlData.getCodTime() + "),\n");
            System.out.println("Growth rate " + uf + "-" + year + rate);
        }
        System.out.println("-------------------- INSERT CRESCIMENTO ------------------------");
        System.out.println(insertCrescimento);
    }

    public static HashMap<String, EtlData> collectDataMicroDados() {
        StringBuilder pathZipsActual;
        StringBuilder pathUnzipActual;
        HashMap<String, EtlData> etlDataMap = new HashMap<>();
        List<Integer> yearsSearch = new ArrayList<>(List.of(2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018));
        for (Integer yearActual : yearsSearch) {
            String yearActualString = yearActual.toString();
            pathZipsActual = new StringBuilder(pathZips);
            pathUnzipActual = new StringBuilder(pathUnzips);
            String ftpLink = "https://download.inep.gov.br/dados_abertos/microdados_censo_escolar_" + yearActual + ".zip";
            String nameFile = yearActual.toString() + "md";
            String localFilePath = pathZipsActual + yearActualString + "/" +  nameFile + ".zip";
            try {
                downloadZip(localFilePath, ftpLink, yearActual + "md.zip", true);
                System.out.println("File downloaded successfully.");

                // Unzip the downloaded file
                unzipFile(localFilePath, pathUnzipActual.append(yearActual).append("/").toString(), nameFile, "csv", MICRO_DADOS_VALIDATION);
                System.out.println("File unzipped successfully.");

                String csvFile = pathUnzipActual.append(nameFile).append(".csv").toString(); // Replace with the actual path to your CSV file
                String line;
                String csvSeparator = ";";

                try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                    // Read the CSV file line by line
                    while ((line = br.readLine()) != null) {
                        if (line.contains("NU_ANO_CENSO"))
                            continue;
                        String[] data = line.split(csvSeparator);

                        // Extract the required column values
                        String nuAnoCenso = data[0];
                        String sgUf = data[4];
                        String inInternet = data[187];
                        String key = sgUf + "-" + nuAnoCenso;
                        EtlData etlData = etlDataMap.get(key);
                        // Create an instance of EtlData and populate it with the extracted values
                        if (etlData == null) {
                            etlData = new EtlData();
                            etlData.setNuAnoCenso(nuAnoCenso);
                            etlData.setUf(sgUf);
                            etlData.setQuantEscolas(1.0);
                            etlData.setQuantEscolasAcesso(equals(inInternet, "1") ? 1.0 : 0.0); //Sum schools with internet access
                            etlData.setInInternet(inInternet);
                        }
                        else {
                            etlData.setQuantEscolas(etlData.getQuantEscolas() + 1.0);
                            etlData.setQuantEscolasAcesso(equals(inInternet, "1") ? etlData.getQuantEscolasAcesso() + 1.0 : etlData.getQuantEscolasAcesso()); //Sum schools with internet access
                        }
                        // Add the EtlData object to the list
                        etlDataMap.put(key, etlData);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }



            } catch (IOException e) {
                e.printStackTrace();
            }
            // Print the EtlData objects
            Double countQuantEscolas = 0.0;
            Double countQuantEscolasAcesso = 0.0;
            for (EtlData etlData : etlDataMap.values()) {
                countQuantEscolasAcesso += etlData.getQuantEscolasAcesso();
                countQuantEscolas += etlData.getQuantEscolas();
                System.out.println(etlData);
            }
            System.out.println("QuantEscolas Tot:"+countQuantEscolas);
            System.out.println("QuantEscolasAcesso Tot:"+countQuantEscolasAcesso);
        }

        return etlDataMap;
    }

    public static HashMap<String, EtlData> collectDataPIB(HashMap<String, EtlData> etlDataMap) {
        String ftpLink = "https://ftp.ibge.gov.br/Pib_Municipios/2018/base/base_de_dados_2010_2018_xls.zip";
        String nameFile = "pib";
        String localFilePath = pathZips +  nameFile + ".zip";
        try {
            downloadZip(localFilePath, ftpLink, nameFile + ".zip");
            System.out.println("File downloaded successfully.");

            // Unzip the downloaded file
            unzipFile(localFilePath, pathUnzips, nameFile, "xls", null);
            System.out.println("File unzipped successfully.");

            String xlsFile = pathUnzips + nameFile + ".xls";

            try (FileInputStream fis = new FileInputStream(xlsFile);
                 Workbook workbook = new HSSFWorkbook(fis)) {
                Sheet sheet = workbook.getSheetAt(0); // Cause the values is in the first sheet
                boolean firstElementIgnored = false;
                for (int i=5566; i <= sheet.getLastRowNum(); i++) {
                    Cell year = sheet.getRow(i).getCell(0);
                    Cell uf = sheet.getRow(i).getCell(4);
                    String key = uf.getStringCellValue() + "-" + (int) year.getNumericCellValue();
                    if (!firstElementIgnored) {
                        firstElementIgnored = true;
                        System.out.println(key);
                    }
                    EtlData obj = etlDataMap.get(key);
                    if (obj == null) {
                        System.out.println("this key is null -> " + key);
                        continue;
                    }

                    obj.setPib(add(obj.getPib(), BigDecimal.valueOf(sheet.getRow(i).getCell(38).getNumericCellValue())));
                    etlDataMap.put(key, obj);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        BigDecimal pib = BigDecimal.ZERO;
        EtlData firstEtl = etlDataMap.entrySet().stream().findFirst().orElse(null).getValue();
        String key = firstEtl.getUf().toUpperCase() + "-" + firstEtl.getNuAnoCenso();
        StringBuilder pibs = new StringBuilder();
        for (EtlData etlData : etlDataMap.values()) {
            if (!equals(firstEtl.getUf(), etlData.getUf()) || !equals(firstEtl.getNuAnoCenso(), etlData.getNuAnoCenso())) {
                pibs.append("PIB " + firstEtl.getUf() + "-" + firstEtl.getNuAnoCenso() +  ":" + pib + "\n");
                firstEtl = etlData;
                pib = BigDecimal.ZERO;
            }
            pib = add(pib, etlData.getPib());
            System.out.println(etlData);
        }
        System.out.println(pibs);
        return etlDataMap;
    }

    public static void createEtlAcessos(StringBuilder pathEtlActual, Integer yearActual, List<EtlData> etls, StringBuilder insertAcessos) {
        Workbook outputWorkbook = new HSSFWorkbook();
        Sheet accessSheet = outputWorkbook.createSheet("Output");
        Row nameColluns = accessSheet.createRow(0);
        // Seting name collums
        nameColluns.createCell(0).setCellValue("codTempo");
        nameColluns.createCell(1).setCellValue("codEstado");
        nameColluns.createCell(2).setCellValue("QuantDomicilios");
        nameColluns.createCell(3).setCellValue("QuantDomiciliosAcesso");
        nameColluns.createCell(4).setCellValue("QuantEscolas");
        nameColluns.createCell(5).setCellValue("QuantEscolasAcesso");
        nameColluns.createCell(6).setCellValue("pib");

        for (int i = 1; i <= codStates.length; i++) {
            Row valueRow = accessSheet.createRow(i);
            EtlData obj = etls.get(i-1);
            valueRow.createCell(0).setCellValue(obj.getCodTime());
            valueRow.createCell(1).setCellValue(obj.getCodState());
            valueRow.createCell(2).setCellValue(obj.getQuantDomicilios());
            valueRow.createCell(3).setCellValue(obj.getQuantDomiciliosAcesso());
            valueRow.createCell(4).setCellValue(obj.getQuantEscolas());
            valueRow.createCell(5).setCellValue(obj.getQuantEscolasAcesso());
            valueRow.createCell(6).setCellValue(Double.parseDouble(obj.getPib().toString()));

            insertAcessos.append("(" + obj.getCodTime() + ", " + obj.getCodState() + ", " + obj.getQuantDomicilios() + ", "
                    + obj.getQuantDomiciliosAcesso() + ", " + obj.getQuantEscolas() + ", " + obj.getQuantEscolasAcesso() + ", " + obj.getPib() + "),\n");
        }

        File directoryEtl = new File(pathEtlActual.append(yearActual).append("/").toString());
        if (!directoryEtl.exists()) {
            directoryEtl.mkdirs();
            //continue;
        }

        try (FileOutputStream fos = new FileOutputStream(directoryEtl + "/" + renameFile(null, yearActual.toString()))) {
            outputWorkbook.write(fos);
            System.out.println("Output file created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<EtlData> fillEtls(Workbook workbook, Integer yearActual, String searchValue, HashMap<String, EtlData> etlDataHashMap) {
        List<EtlData> etls = new ArrayList<>();
        Sheet sheet;
        sheet = workbook.getSheetAt(7);

        EtlData obj;
        for (Row row : sheet) {
            Cell cell = row.getCell(0); // Cause the value in the first column
            String cellValue;
            if (cell == null || cell.getCellTypeEnum() != CellType.STRING || strEmpty(cell.getStringCellValue()) || !nameStates.contains(cellValue = cell.getStringCellValue().trim())) {
                continue;
            }

            Cell totalCell = row.getCell(1);
            Cell quantAcessoCell = row.getCell(2);

            if (totalCell == null || quantAcessoCell == null) {
                continue;
            }
            String key = getUfByNameState(cellValue).toUpperCase() + "-" + yearActual;
            obj = etlDataHashMap.get(key);
            if (obj == null)
                System.out.println("CellValue -> " + cellValue + " this key dont exists -> " + key);
            obj.setQuantDomicilios(totalCell.getNumericCellValue());
            obj.setQuantDomiciliosAcesso(quantAcessoCell.getNumericCellValue());
            etlDataHashMap.put(key, obj);
            etls.add(obj);
        }
        return etls;
    }

    public static void downloadZip(String localFilePath, String ftpLink, String nameArchive) throws IOException {
        downloadZip(localFilePath, ftpLink, nameArchive, false);
    }

    public static void downloadZip(String localFilePath, String ftpLink, String nameArchive, boolean disableSSL) throws IOException {
        File directoryFileDownloaded = new File(localFilePath);
        if (directoryFileDownloaded.exists()) {
            System.out.println(nameArchive + " already downloaded!");
            return;
        }

        if (disableSSL)
            disableSSLVerification();
        URL url = new URL(ftpLink);
        URLConnection connection = url.openConnection();
        InputStream inputStream = connection.getInputStream();

        FileOutputStream outputStream = new FileOutputStream(localFilePath);
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.close();
        inputStream.close();
    }

    private static void unzipFile(String zipFilePath, String destDir, String uf, String extension) throws IOException {
        unzipFile(zipFilePath, destDir, uf, extension, 1);
    }
    private static void unzipFile(String zipFilePath, String destDir, String uf, String extension, Integer validation) throws IOException {
        File directoryFileDownloaded = new File(destDir + uf + extension);
        if (directoryFileDownloaded.exists()) {
            System.out.println(destDir + " already unziped!");
            return;
        }

        Path destPath = Paths.get(destDir);
        if (!Files.exists(destPath)) {
            Files.createDirectories(destPath);
        }
        System.out.println(zipFilePath);
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(Paths.get(zipFilePath)), StandardCharsets.ISO_8859_1)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = new String(entry.getName().getBytes(), StandardCharsets.ISO_8859_1);
                Path entryPath = destPath.resolve(renameFile(extension, uf));

                //Validation if is the correct entry
                if (Objects.equals(validation, PNAD_VALIDATION)) {
                    if (!entryName.replaceAll(" ", "").contains("tab7_7.xls") && !entryName.replaceAll(" ", "").contains("Tabela7.7")
                            && (!entryName.contains("PARTE5") || entryName.contains("CV")) && (!entryName.contains("5.") || !entryName.contains("VALOR")))
                        continue;
                }
                else if (Objects.equals(validation, MICRO_DADOS_VALIDATION)) {
                    if (!entryName.endsWith(".csv"))
                        continue;
                }


                if (!Files.exists(entryPath)) {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zipInputStream, entryPath);
                }
                zipInputStream.closeEntry();
            }
        }

        /*try (ZipFile zipFile = new ZipFile(zipFilePath, StandardCharsets.ISO_8859_1)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String entryName = renameFile(entry.getName(), uf);
                    if (isValidEntryName(entryName)) {
                        Path entryPath = destPath.resolve(entryName);

                        Files.createDirectories(entryPath.getParent());

                        try (InputStream inputStream = zipFile.getInputStream(entry);
                             FileOutputStream outputStream = new FileOutputStream(entryPath.toFile())) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }
                    } else {
                        System.err.println("Skipping invalid entry name: " + entry.getName());
                    }
                }
            }
        }*/
    }

    private static void disableSSLVerification() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            // Create an SSL context with the trust manager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            // Set the SSL context on the default HTTPS connection factory
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            // Disable hostname verification
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, sslSession) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getUfByNameState(String nameState) {
        return switch (nameState) {
            case "Rondônia" -> "ro";
            case "Acre"-> "ac";
            case "Amazonas"-> "am";
            case "Roraima"-> "rr";
            case "Pará"-> "pa";
            case "Amapá"-> "ap";
            case "Tocantins"-> "to";
            case "Maranhão"-> "ma";
            case "Piauí"-> "pi";
            case "Ceará"-> "ce";
            case "Rio Grande do Norte"-> "rn";
            case "Paraíba"-> "pb";
            case "Pernambuco"-> "pe";
            case "Alagoas"-> "al";
            case "Sergipe"-> "se";
            case "Bahia"-> "ba";
            case "Minas Gerais"-> "mg";
            case "Espírito Santo"-> "es";
            case "Rio de Janeiro"-> "rj";
            case "São Paulo"-> "sp";
            case "Paraná"-> "pr";
            case "Santa Catarina"-> "sc";
            case "Rio Grande do Sul"-> "rs";
            case "Mato Grosso do Sul"-> "ms";
            case "Mato Grosso"-> "mt";
            case "Goiás"-> "go";
            case "Distrito Federal"-> "df";
            default -> null;
        };
    }
    public static BigDecimal add(BigDecimal value, BigDecimal sum) {
        if (value == null)
            value = BigDecimal.ZERO;
        if (sum == null)
            sum = BigDecimal.ZERO;
        return value.add(sum);
    }
    private static boolean equals(String str1, String str2) {
        if (str1 == null)
            str1 = "";
        if (str2 == null)
            str2 = "";
        return str1.equals(str2);
    }
    private static boolean strEmpty(String str) {
        return str == null || str.equals("");
    }
    private static String renameFile(String extension, String prefix) {
        if (extension == null)
            extension = "xls";
        return prefix + "." + extension;
    }

    public static String removeAccents(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}