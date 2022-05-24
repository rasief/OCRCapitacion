package ocrcapitacion;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 *
 * @author Feisar Moreno
 * @date 22/06/2018
 */
public class OCRCapitacion {
    private static final String GS = "C:\\Program Files\\gs\\gs9.53.3\\bin\\gswin64c.exe";
    private static final String TS = "C:\\Program Files\\Tesseract-OCR\\tesseract.exe";
    private static final String RAIZ = "NEPS\\12 NEPS DICIEMBRE 2020";
    //private static final String RAIZ = "archivos";
    private static PrintWriter PW;
    
    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        //Se crea el archivo de salida
        PW = new PrintWriter("archivos\\diciembre_2020_pdf.csv");
        
        leerCarpeta();
        
        PW.close();
    }
    
    private static void leerCarpeta() throws IOException, InterruptedException {
        File carpeta = new File(RAIZ);
        leerCarpeta(carpeta);
    }
    
    private static void leerCarpeta(File carpeta) throws IOException, InterruptedException {
        if (!carpeta.isDirectory()) {
            throw new IllegalArgumentException("The param is not a folder");
        } else {
            File[] listaArchivos = carpeta.listFiles();
            for (File archAux : listaArchivos) {
                //si se trata de una carpeta, se debe abrir
                if (archAux.isDirectory()) {
                    leerCarpeta(archAux);
                } else {
                    //Se valida si se debe cargar el archivo
                    if (validarArchivo(archAux.getName())) {
                        leerArchivo(archAux.getAbsolutePath());
                    }
                }
            }
        }
    }
    
    private static void leerArchivo(String nombreArchivo) throws IOException, InterruptedException {
        //System.out.println(nombreArchivo);
        ProcessBuilder ps = new ProcessBuilder(
                "\"" + GS + "\"",
                "-dNOPAUSE",
                "-sDEVICE=jpeg",
                "-dUseCIEColor",
                "-dTextAlphaBits=4",
                "-dGraphicsAlphaBits=4",
                "-r300x300",
                "-sOutputFile=\"imagenes\\archivo_salida%d.jpg\"",
                "\"" + nombreArchivo + "\"",
                "-c",
                "quit");

        ps.redirectErrorStream(true);
        
        Process pr = ps.start();
        pr.waitFor();
        //System.out.println("Ghostscript end");
        
        //Se crean las imágenes de regiones de la imagen base
        crearSubimagenes();
        
        //Id
        ArrayList<String> listaId = hacerOCR("imagenes\\archivo_id.png");
        
        String id = "";
        for (String linea : listaId) {
            if (!linea.equals("")) {
                id = linea;
                break;
            }
        }
        id = id.replaceAll("\\s", "").replace("O", "0").replace("Q", "9").replace("(5", "6");
        
        //Fecha
        ArrayList<String> listaFecha = hacerOCR("imagenes\\archivo_fecha.png");
        
        String fecha = "";
        for (String linea : listaFecha) {
            if (linea.length() >= 10) {
                try {
                    Integer.parseInt(linea.substring(0, 2), 10);
                } catch (NumberFormatException ex) {
                    continue;
                }
                
                fecha = linea;
                break;
            }
        }
        if (fecha.length() >= 11 && !fecha.substring(10, 11).equals(" ")) {
            fecha = fecha.substring(0, 10) + " " + fecha.substring(10);
        }
        if (fecha.length() >= 16 && fecha.substring(13, 14).equals("2")) {
            fecha = fecha.substring(0, 13) + ":" + fecha.substring(14);
        }
        
        //Basal FEV1
        File filaAux = new File("imagenes\\archivo_basal_fev1.png");
        String basalFEV1 = "";
        if (filaAux.exists()) {
            ArrayList<String> listaAux = hacerOCR("imagenes\\archivo_basal_fev1.png");
            
            for (String linea : listaAux) {
                if (!linea.equals("")) {
                    basalFEV1 = linea.replace("*", "").replace("o", "0").replace("O", "0").replace("s", "8").replace("S", "8").trim();
                    break;
                }
            }
        }
        
        //FEV1/FVC
        filaAux = new File("imagenes\\archivo_fev1_fvc.png");
        String FEV1FVC = "";
        if (filaAux.exists()) {
            ArrayList<String> listaAux = hacerOCR("imagenes\\archivo_fev1_fvc.png");
            
            for (String linea : listaAux) {
                if (!linea.equals("")) {
                    FEV1FVC = linea.replace("*", "").replace("o", "0").replace("O", "0").replace(".", ",").trim();
                    break;
                }
            }
        }
        
        //Post FEV1
        filaAux = new File("imagenes\\archivo_post_fev1.png");
        String postFEV1 = "";
        if (filaAux.exists()) {
            ArrayList<String> listaAux = hacerOCR("imagenes\\archivo_post_fev1.png");
            
            for (String linea : listaAux) {
                if (!linea.equals("")) {
                    postFEV1 = linea.replace("*", "").replace("o", "0").replace("O", "0").trim();
                    break;
                }
            }
        }
        
        ArrayList<String> listaDatos = new ArrayList<>();
        listaDatos.add(nombreArchivo);
        listaDatos.add(id);
        listaDatos.add(fecha);
        listaDatos.add(basalFEV1);
        listaDatos.add(FEV1FVC);
        listaDatos.add(postFEV1);
        procesarLista(listaDatos);
    }
    
    private static boolean validarArchivo(String nombArchivo) {
        boolean cargar = false;
        String extension = "";
        int posAux = nombArchivo.lastIndexOf(".");
        if (posAux != -1) {
            extension = nombArchivo.substring(posAux + 1).toLowerCase();
        }
        
        //Solo se acepta si el archivo es pdf
        if (extension.equals("pdf")) {
            cargar = true;
        }
        
        return cargar;
    }
    
    private static void procesarLista(ArrayList<String> listaDatos) {
        String cadenaAux = "";
        for (String textoAux : listaDatos) {
            if (!cadenaAux.equals("")) {
                cadenaAux += ";";
            }
            cadenaAux += textoAux;
        }
        PW.println(cadenaAux);
        System.out.println(cadenaAux);
    }
    
    private static int[] crearSubimagenes() throws MalformedURLException, IOException, InterruptedException {
        BufferedImage imgBase1 = ImageIO.read(new File("imagenes\\archivo_salida1.jpg"));
        BufferedImage imgBase2 = ImageIO.read(new File("imagenes\\archivo_salida2.jpg"));
        
        File archAux;
        
        //Se busca la etiqueta "ID:"
        BufferedImage imgTagID = new BufferedImage(150, 40, BufferedImage.TYPE_INT_ARGB);
        
        int yID = 0;
        for (int pasoAux = 360; pasoAux <= 500; pasoAux += 10) {
            yID = pasoAux;
            
            for (int x = 0; x < imgTagID.getWidth(); x++) {
                for (int y = 0; y < imgTagID.getHeight(); y++) {
                    imgTagID.setRGB(x, y, imgBase1.getRGB(x + 1250, y + yID));
                }
            }

            archAux = new File("imagenes\\archivo_tag_id.png");
            ImageIO.write(imgTagID, "PNG", archAux);
            
            ArrayList<String> listaTagID = hacerOCR("imagenes\\archivo_tag_id.png");
            if (listaTagID.size() > 0 && listaTagID.get(0).length() >= 2) {
                String textoAux = listaTagID.get(0).trim().split("\\s+")[0];
                if (textoAux.equalsIgnoreCase("ID")) {
                    yID += 10;
                    break;
                }
            }
        }
        
        //Imagen con el Id
        BufferedImage imgId = new BufferedImage(650, 45, BufferedImage.TYPE_INT_ARGB);
        
        for (int x = 0; x < imgId.getWidth(); x++) {
            for (int y = 0; y < imgId.getHeight(); y++) {
                imgId.setRGB(x, y, imgBase1.getRGB(x + 1780, y + yID));
            }
        }
        
        archAux = new File("imagenes\\archivo_id.png");
        ImageIO.write(imgId, "PNG", archAux);
        
        //Imagen con la fecha
        BufferedImage imgFecha = new BufferedImage(500, 100, BufferedImage.TYPE_INT_ARGB);
        
        for (int x = 0; x < imgFecha.getWidth(); x++) {
            for (int y = 0; y < imgFecha.getHeight(); y++) {
                imgFecha.setRGB(x, y, imgBase1.getRGB(x + 625, y + yID + 480));
            }
        }
        
        archAux = new File("imagenes\\archivo_fecha.png");
        ImageIO.write(imgFecha, "PNG", archAux);
        
        //Se busca la linea FEV1
        BufferedImage imgFEV1Base = new BufferedImage(200, 40, BufferedImage.TYPE_INT_ARGB);
        
        int yFEV1 = 0;
        boolean halladoFEV = false;
        for (int pasoAux = 1550; pasoAux <= 2050; pasoAux += 10) {
            yFEV1 = pasoAux;
            
            for (int x = 0; x < imgFEV1Base.getWidth(); x++) {
                for (int y = 0; y < imgFEV1Base.getHeight(); y++) {
                    imgFEV1Base.setRGB(x, y, imgBase2.getRGB(x + 125, y + yFEV1));
                }
            }

            archAux = new File("imagenes\\archivo_fev1_base.png");
            ImageIO.write(imgFEV1Base, "PNG", archAux);
            
            ArrayList<String> listaFEV1 = hacerOCR("imagenes\\archivo_fev1_base.png");
            if (listaFEV1.size() > 0 && listaFEV1.get(0).length() >= 5) {
                String textoAux = listaFEV1.get(0).substring(0, 4);
                if (textoAux.equalsIgnoreCase("FEV1") && (listaFEV1.get(0).toUpperCase().contains("L"))) {
                    halladoFEV = true;
                    break;
                }
            }
        }
        
        int tipoATS = 1;
        if (yFEV1 > 0) {
            //Se verifica si es un archivo de 3 o 6 columnas de resultados
            File archTipoATS = new File("imagenes\\archivo_tipo_ats.png");
            BufferedImage imgTipoATS = new BufferedImage(300, 40, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < imgTipoATS.getWidth(); x++) {
                for (int y = 0; y < imgTipoATS.getHeight(); y++) {
                    imgTipoATS.setRGB(x, y, imgBase2.getRGB(x + 788, y + yFEV1 - 142));
                }
            }
            ImageIO.write(imgTipoATS, "PNG", archTipoATS);
            
            ArrayList<String> listaTipoATS = hacerOCR("imagenes\\archivo_tipo_ats.png");
            if (listaTipoATS.size() > 0) {
                String textoAux = listaTipoATS.get(0).toUpperCase();
                if (textoAux.contains("ATS")) {
                    tipoATS = 2;
                }
            }

        }
        
        File archBasalFEV1 = new File("imagenes\\archivo_basal_fev1.png");
        archBasalFEV1.delete();
        File archFEV1FVC = new File("imagenes\\archivo_fev1_fvc.png");
        archFEV1FVC.delete();
        File archPostFEV1 = new File("imagenes\\archivo_post_fev1.png");
        archPostFEV1.delete();
        
        if (halladoFEV) {
            //Se crean las imagenes para los datos FEV1 (L) y FEV1/FVC
            BufferedImage imgBasalFEV1, imgFEV1FVC, imgPostFEV1;
            switch (tipoATS) {
                case 1:
                    //Basal FEV1
                    imgBasalFEV1 = new BufferedImage(150, 40, BufferedImage.TYPE_INT_ARGB);
                    
                    for (int x = 0; x < imgBasalFEV1.getWidth(); x++) {
                        for (int y = 0; y < imgBasalFEV1.getHeight(); y++) {
                            imgBasalFEV1.setRGB(x, y, imgBase2.getRGB(x + 1137, y + yFEV1));
                        }
                    }
                    ImageIO.write(imgBasalFEV1, "PNG", archBasalFEV1);
                    
                    //FEV1/FVC
                    imgFEV1FVC = new BufferedImage(150, 40, BufferedImage.TYPE_INT_ARGB);
                    
                    for (int x = 0; x < imgFEV1FVC.getWidth(); x++) {
                        for (int y = 0; y < imgFEV1FVC.getHeight(); y++) {
                            imgFEV1FVC.setRGB(x, y, imgBase2.getRGB(x + 1405, y + yFEV1 + 50));
                        }
                    }
                    ImageIO.write(imgFEV1FVC, "PNG", archFEV1FVC);
                    
                    //Post FEV1
                    imgPostFEV1 = new BufferedImage(150, 40, BufferedImage.TYPE_INT_ARGB);

                    for (int x = 0; x < imgPostFEV1.getWidth(); x++) {
                        for (int y = 0; y < imgPostFEV1.getHeight(); y++) {
                            imgPostFEV1.setRGB(x, y, imgBase2.getRGB(x + 1637, y + yFEV1));
                        }
                    }
                    ImageIO.write(imgPostFEV1, "PNG", archPostFEV1);
                    break;
                    
                case 2:
                    //No contiene basal FEV1
                    
                    //FEV1/FVC
                    imgFEV1FVC = new BufferedImage(150, 40, BufferedImage.TYPE_INT_ARGB);
                    
                    for (int x = 0; x < imgFEV1FVC.getWidth(); x++) {
                        for (int y = 0; y < imgFEV1FVC.getHeight(); y++) {
                            imgFEV1FVC.setRGB(x, y, imgBase2.getRGB(x + 787, y + yFEV1 + 50));
                        }
                    }
                    ImageIO.write(imgFEV1FVC, "PNG", archFEV1FVC);
                    
                    //Post FEV1
                    imgPostFEV1 = new BufferedImage(150, 40, BufferedImage.TYPE_INT_ARGB);
                    
                    for (int x = 0; x < imgPostFEV1.getWidth(); x++) {
                        for (int y = 0; y < imgPostFEV1.getHeight(); y++) {
                            imgPostFEV1.setRGB(x, y, imgBase2.getRGB(x + 1707, y + yFEV1));
                        }
                    }
                    ImageIO.write(imgPostFEV1, "PNG", archPostFEV1);
                    break;
            }
        } else {
            yFEV1 = 0;
        }
        
        int[] arrResultado = {yFEV1, tipoATS};
        
        return arrResultado;
    }
    
    private static ArrayList<String> hacerOCR(String nombreArchivo) throws IOException, InterruptedException {
        /*ProcessBuilder ps = new ProcessBuilder(
                "\"" + TS + "\"",
                nombreArchivo,
                "-l",
                "spa",
                "-psm",
                "11",
                "stdout");
        
        ps.redirectErrorStream(true);
        
        Process pr = ps.start();
        
        ArrayList<String> listaSalida = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                listaSalida.add(linea);
            }
            pr.waitFor();
        }
        
        return listaSalida;*/
        Tesseract tesseract = new Tesseract(); 
        try {
            tesseract.setDatapath("..\\Tess4J\\tessdata"); 
            
            String textoAux = tesseract.doOCR(new File(nombreArchivo));
            
            String[] arrAux = textoAux.split(((char)10) + "");
            ArrayList<String> listaSalida = new ArrayList<>(Arrays.asList(arrAux));
            
            return listaSalida;
        } catch (TesseractException e) { 
            return new ArrayList<>();
        }
    }
}
