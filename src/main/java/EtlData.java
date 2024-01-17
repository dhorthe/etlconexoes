import java.math.BigDecimal;

public class EtlData {
    private String uf;
    private String state;
    private Double quantDomicilios;
    private Double quantEscolas;
    private Double quantDomiciliosAcesso;
    private Double quantEscolasAcesso;
    private String nuAnoCenso;
    private String inInternet;
    private BigDecimal pib;

    public EtlData() {
    }

    @Override
    public String toString() {
        return "EtlData{" +
                "nuAnoCenso='" + nuAnoCenso + '\'' +
                ", uf ='" + uf + '\'' +
                ", quantDomicilios='" + quantDomicilios + '\'' +
                ", quantDomiciliosAcesso='" + quantDomiciliosAcesso + '\'' +
                ", quantEscolas='" + quantEscolas + '\'' +
                ", quantEscolasAcesso='" + quantEscolasAcesso + '\'' +
                ", inInternet='" + inInternet + '\'' +
                '}';
    }

    public String getUf() {
        return uf;
    }

    public Integer getCodTime() {
        return switch (nuAnoCenso) {
            case "2011" -> 1;
            case "2012" -> 2;
            case "2013" -> 3;
            case "2014" -> 4;
            case "2015" -> 5;
            case "2016" -> 6;
            case "2017" -> 7;
            case "2018" -> 8;
            default -> null;
        };
    }

    public String getCodState() {
        return switch (uf.toLowerCase()) {
            case "ro" -> "11";
            case "ac"-> "12";
            case "am"-> "13";
            case "rr"-> "14";
            case "pa"-> "15";
            case "ap"-> "16";
            case "to"-> "17";
            case "ma"-> "21";
            case "pi"-> "22";
            case "ce"-> "23";
            case "rn"-> "24";
            case "pb"-> "25";
            case "pe"-> "26";
            case "al"-> "27";
            case "se"-> "28";
            case "ba"-> "29";
            case "mg"-> "31";
            case "es"-> "32";
            case "rj"-> "33";
            case "sp"-> "35";
            case "pr"-> "41";
            case "sc"-> "42";
            case "rs"-> "43";
            case "ms"-> "50";
            case "mt"-> "51";
            case "go"-> "52";
            case "df"-> "53";
            default -> null;
        };
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Double getQuantDomicilios() {
        return quantDomicilios;
    }

    public void setQuantDomicilios(Double quantDomicilios) {
        this.quantDomicilios = quantDomicilios;
    }

    public Double getQuantEscolas() {
        return quantEscolas;
    }

    public void setQuantEscolas(Double quantEscolas) {
        this.quantEscolas = quantEscolas;
    }

    public Double getQuantDomiciliosAcesso() {
        return quantDomiciliosAcesso;
    }

    public void setQuantDomiciliosAcesso(Double quantDomiciliosAcesso) {
        this.quantDomiciliosAcesso = quantDomiciliosAcesso;
    }

    public Double getQuantEscolasAcesso() {
        return quantEscolasAcesso;
    }

    public void setQuantEscolasAcesso(Double quantEscolasAcesso) {
        this.quantEscolasAcesso = quantEscolasAcesso;
    }

    public String getNuAnoCenso() {
        return nuAnoCenso;
    }

    public void setNuAnoCenso(String nuAnoCenso) {
        this.nuAnoCenso = nuAnoCenso;
    }

    public String getInInternet() {
        return inInternet;
    }

    public void setInInternet(String inInternet) {
        this.inInternet = inInternet;
    }

    public BigDecimal getPib() {
        return pib;
    }

    public void setPib(BigDecimal pib) {
        this.pib = pib;
    }
}
