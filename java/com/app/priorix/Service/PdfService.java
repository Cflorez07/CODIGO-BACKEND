package com.app.priorix.Service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.app.priorix.model.entity.HistoriaClinica;
import com.app.priorix.model.entity.Paciente;
import com.app.priorix.model.entity.Triaje;
import com.app.priorix.model.entity.Usuario;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

/**
 * Servicio centralizado para la generación de documentos PDF en PRIORIX
 * con identidad visual institucional uniforme (logo oficial, colores institucionales,
 * encabezado, numeración y pie de página).
 */
@Service
public class PdfService {

    // Paleta institucional de PRIORIX
    public static final Color COLOR_NAVY = new Color(11, 22, 38);          // #0B1626
    public static final Color COLOR_SURFACE = new Color(17, 31, 51);        // #111F33
    public static final Color COLOR_INSTITUCIONAL = new Color(91, 159, 232); // #5B9FE8
    public static final Color COLOR_DARK_BLUE = new Color(10, 99, 196);     // #0A63C4
    public static final Color COLOR_MUTED = new Color(122, 146, 168);       // #7A92A8
    public static final Color COLOR_BG_LIGHT = new Color(245, 248, 252);    // Fondo suave tarjetas/tablas
    public static final Color COLOR_BORDER = new Color(210, 226, 242);      // Bordes sutiles
    public static final Color COLOR_TEXT = new Color(20, 35, 55);           // Texto legible

    private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Obtiene la imagen del logo institucional oficial de PRIORIX para los encabezados de reportes y documentos PDF.
     * Carga el archivo institucional priorix-logo.png de alta resolución y proporciones óptimas.
     */
    public Image obtenerLogoOficialPriorix() {
        try {
            // 1. Intentar cargar desde el classpath (recurso estático)
            java.io.InputStream is = getClass().getResourceAsStream("/static/img/priorix-logo.png");
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                is.close();
                Image img = Image.getInstance(bytes);
                img.scaleToFit(180, 44);
                return img;
            }

            // 2. Intentar cargar desde el sistema de archivos
            String[] posiblesRutas = {
                "src/main/resources/static/img/priorix-logo.png",
                "target/classes/static/img/priorix-logo.png"
            };
            for (String ruta : posiblesRutas) {
                File f = new File(ruta);
                if (f.exists()) {
                    Image img = Image.getInstance(f.getAbsolutePath());
                    img.scaleToFit(180, 44);
                    return img;
                }
            }
        } catch (Exception ignored) {}

        // 3. Fallback de respaldo en memoria si no estuviese el archivo en disco
        try {
            int width = 360;
            int height = 80;
            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = bi.createGraphics();

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color c1 = new Color(10, 99, 196);
            Color c2 = new Color(91, 159, 232);

            g.setColor(c1);
            g.fillRoundRect(24, 8, 22, 64, 10, 10);
            g.fillRoundRect(8, 28, 54, 24, 10, 10);

            g.setColor(c2);
            g.fillOval(66, 18, 9, 9);
            g.fillOval(66, 36, 9, 9);
            g.fillOval(66, 54, 9, 9);

            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            GeneralPath path = new GeneralPath();
            path.moveTo(10, 40);
            path.lineTo(20, 40);
            path.lineTo(25, 30);
            path.lineTo(31, 52);
            path.lineTo(37, 20);
            path.lineTo(43, 48);
            path.lineTo(48, 40);
            path.lineTo(58, 40);
            g.draw(path);

            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 30));
            g.setColor(COLOR_NAVY);
            g.drawString("PRIORI", 86, 42);

            g.setColor(COLOR_INSTITUCIONAL);
            g.drawString("X", 216, 42);

            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 9));
            g.setColor(COLOR_MUTED);
            g.drawString("SERVICIO DE URGENCIAS HOSPITALARIAS", 88, 58);

            g.dispose();

            Image img = Image.getInstance(bi, null);
            img.scaleToFit(180, 44);
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Inserta el encabezado institucional estándar de PRIORIX en cualquier documento PDF.
     */
    public void agregarEncabezadoInstitucional(Document doc, String tituloDoc, String subtituloDoc) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{60, 40});
        headerTable.setSpacingAfter(6);

        // Celda izquierda: Logo + Eslogan
        PdfPCell cellLeft = new PdfPCell();
        cellLeft.setBorder(Rectangle.NO_BORDER);
        cellLeft.setPadding(0);

        Image logo = obtenerLogoOficialPriorix();
        if (logo != null) {
            cellLeft.addElement(logo);
        } else {
            Font fTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_NAVY);
            cellLeft.addElement(new Paragraph("PRIORIX - SERVICIO DE URGENCIAS", fTitle));
        }

        Font fSlogan = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8.5f, COLOR_MUTED);
        Paragraph pSlogan = new Paragraph("«Priorizamos tu salud, agilizamos tu atención»", fSlogan);
        pSlogan.setSpacingBefore(2);
        cellLeft.addElement(pSlogan);
        headerTable.addCell(cellLeft);

        // Celda derecha: Datos institucionales de emisión
        PdfPCell cellRight = new PdfPCell();
        cellRight.setBorder(Rectangle.NO_BORDER);
        cellRight.setPadding(0);
        cellRight.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font fMetaLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, COLOR_NAVY);
        Font fMetaVal = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXT);

        Paragraph pMeta = new Paragraph();
        pMeta.setAlignment(Element.ALIGN_RIGHT);
        pMeta.add(new Chunk("SISTEMA DE GESTIÓN PRIORIX\n", fMetaLabel));
        pMeta.add(new Chunk("Emisión: ", fMetaLabel));
        pMeta.add(new Chunk(LocalDateTime.now().format(FMT_DATETIME) + "\n", fMetaVal));
        pMeta.add(new Chunk("Validez: ", fMetaLabel));
        pMeta.add(new Chunk("Documento Oficial Verificado", fMetaVal));
        cellRight.addElement(pMeta);
        headerTable.addCell(cellRight);

        doc.add(headerTable);

        // Línea divisoria decorativa institucional (#5B9FE8)
        PdfPTable lineTable = new PdfPTable(1);
        lineTable.setWidthPercentage(100);
        lineTable.setSpacingBefore(3);
        lineTable.setSpacingAfter(8);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBackgroundColor(COLOR_INSTITUCIONAL);
        lineCell.setFixedHeight(2.5f);
        lineCell.setBorder(Rectangle.NO_BORDER);
        lineTable.addCell(lineCell);
        doc.add(lineTable);

        // Bloque del Título del Documento
        PdfPTable titleBanner = new PdfPTable(1);
        titleBanner.setWidthPercentage(100);
        titleBanner.setSpacingAfter(10);

        PdfPCell cTitle = new PdfPCell();
        cTitle.setBackgroundColor(COLOR_BG_LIGHT);
        cTitle.setBorderColor(COLOR_BORDER);
        cTitle.setPaddingTop(6);
        cTitle.setPaddingBottom(7);
        cTitle.setPaddingLeft(10);
        cTitle.setPaddingRight(10);

        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12.5f, COLOR_NAVY);
        Paragraph pT = new Paragraph(tituloDoc.toUpperCase(), fontTitulo);
        cTitle.addElement(pT);

        if (subtituloDoc != null && !subtituloDoc.isBlank()) {
            Font fontSub = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, COLOR_MUTED);
            Paragraph pS = new Paragraph(subtituloDoc, fontSub);
            pS.setSpacingBefore(2);
            cTitle.addElement(pS);
        }

        titleBanner.addCell(cTitle);
        doc.add(titleBanner);
    }

    // =========================================================================
    // 1. REPORTE GENERAL CONSOLIDADO DE ATENCIONES Y TIEMPOS DE ESPERA
    // =========================================================================
    public byte[] generarReportePdf(List<Map<String, Object>> filas, String fi, String ff,
                                    String nivel, String medico, Long promedioEspera, int[] conteos) {
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            InstitucionalFooterEvent footerEvent = new InstitucionalFooterEvent(
                    "Reporte Institucional PRIORIX • Documento Confidencial de Administración Hospitalaria");
            writer.setPageEvent(footerEvent);
            document.open();

            // Encabezado institucional
            String subtituloFiltros = String.format("Filtro: Fechas [%s - %s]  |  Nivel: %s  |  Médico: %s",
                    (fi != null && !fi.isBlank() ? fi : "Inicio"),
                    (ff != null && !ff.isBlank() ? ff : "Hoy"),
                    (nivel != null && !nivel.isBlank() ? nivel : "Todos"),
                    (medico != null && !medico.isBlank() ? medico : "Todos"));

            agregarEncabezadoInstitucional(document, "REPORTE CONSOLIDADO DE ATENCIONES Y TIEMPOS DE ESPERA", subtituloFiltros);

            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_NAVY);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, COLOR_TEXT);
            Font fontKpiVal = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COLOR_DARK_BLUE);

            // Resumen de KPI institucionales
            PdfPTable kpiTable = new PdfPTable(3);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingAfter(12);

            agregarKpiCell(kpiTable, "Total Pacientes Atendidos", String.valueOf(filas.size()), fontBold, fontKpiVal);
            agregarKpiCell(kpiTable, "Tiempo Promedio de Espera", (promedioEspera != null ? promedioEspera + " min" : "N/D"), fontBold, fontKpiVal);
            String dist = String.format("N1: %d | N2: %d | N3: %d | N4: %d | N5: %d",
                    conteos[0], conteos[1], conteos[2], conteos[3], conteos[4]);
            agregarKpiCell(kpiTable, "Distribución Triaje (N1 - N5)", dist, fontBold, fontBold);
            document.add(kpiTable);

            // Tabla de datos
            PdfPTable table = new PdfPTable(new float[]{14, 25, 14, 23, 12, 12});
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            table.setKeepTogether(false);

            String[] headers = {"Fecha", "Paciente", "Nivel Triaje", "Médico Tratante", "Estado", "Espera"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE)));
                cell.setBackgroundColor(COLOR_NAVY);
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorderColor(COLOR_BORDER);
                table.addCell(cell);
            }

            boolean alt = false;
            for (Map<String, Object> r : filas) {
                Color bg = alt ? COLOR_BG_LIGHT : Color.WHITE;
                alt = !alt;

                String fechaStr = r.get("fecha") != null ? r.get("fecha").toString() : "-";
                String pacStr = r.get("paciente") != null ? r.get("paciente").toString() : "-";
                String nivStr = r.get("nivel") != null ? r.get("nivel").toString() : "-";
                String medStr = r.get("medico") != null ? r.get("medico").toString() : "-";
                String estStr = r.get("estado") != null ? r.get("estado").toString() : "-";
                String espStr = r.get("tiempoEspera") != null ? r.get("tiempoEspera") + " min" : "-";

                table.addCell(crearDatoCell(fechaStr, fontNormal, bg, Element.ALIGN_CENTER));
                table.addCell(crearDatoCell(pacStr, fontNormal, bg, Element.ALIGN_LEFT));
                table.addCell(crearDatoCell(nivStr, fontNormal, bg, Element.ALIGN_CENTER));
                table.addCell(crearDatoCell(medStr, fontNormal, bg, Element.ALIGN_LEFT));
                table.addCell(crearDatoCell(estStr, fontNormal, bg, Element.ALIGN_CENTER));
                table.addCell(crearDatoCell(espStr, fontNormal, bg, Element.ALIGN_CENTER));
            }

            if (filas.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No hay atenciones registradas para el criterio seleccionado.", fontNormal));
                emptyCell.setColspan(6);
                emptyCell.setPadding(15);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyCell.setBackgroundColor(COLOR_BG_LIGHT);
                table.addCell(emptyCell);
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el reporte en PDF: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // 2. HISTORIA CLÍNICA DE ATENCIÓN MÉDICA
    // =========================================================================
    public byte[] generarHistoriaClinicaPdf(HistoriaClinica h) {
        Document document = new Document(PageSize.A4, 38, 38, 36, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            InstitucionalFooterEvent footerEvent = new InstitucionalFooterEvent(
                    "Historia Clínica Oficial PRIORIX • Documento Médico Legal Sujeto a Reserva Confidencial");
            writer.setPageEvent(footerEvent);
            document.open();

            Paciente p = h.getPaciente();
            String nombrePac = p != null ? (p.getNombre() + " " + p.getApellido()).trim() : "N.N.";
            String docPac = p != null && p.getDocumento() != null ? p.getDocumento() : "-";

            String subtitulo = "Paciente: " + nombrePac + "  |  Documento: " + docPac + "  |  Folio: " + (h.getId() != null ? h.getId() : "-");
            agregarEncabezadoInstitucional(document, "HISTORIA CLÍNICA DE ATENCIÓN DE URGENCIAS", subtitulo);

            Font fontSub = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, COLOR_NAVY);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, COLOR_TEXT);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, COLOR_NAVY);

            // 1. Identificación del Paciente
            agregarSeccionHeader(document, "1. IDENTIFICACIÓN DEL PACIENTE", fontSub);
            PdfPTable tPac = new PdfPTable(2);
            tPac.setWidthPercentage(100);
            tPac.setSpacingAfter(8);

            String tipoDoc = p != null && p.getTipoDocumento() != null ? p.getTipoDocumento() : "CC";
            String sexoPac = p != null && p.getSexo() != null ? p.getSexo() : "No especificado";
            String nacPac = p != null && p.getFechaNacimiento() != null ? p.getFechaNacimiento().format(FMT_DATE) : "-";
            String fechaAt = h.getFechaAtencion() != null ? h.getFechaAtencion().format(FMT_DATE) : LocalDate.now().format(FMT_DATE);
            String horaAt = h.getHoraAtencion() != null ? h.getHoraAtencion().toString() : "-";
            String eps = p != null && p.getEps() != null ? p.getEps() : "Particular / No asegurado";

            agregarParKeyValue(tPac, "Nombre Completo:", nombrePac, fontBold, fontNormal);
            agregarParKeyValue(tPac, "Documento de Identidad:", tipoDoc + " - " + docPac, fontBold, fontNormal);
            agregarParKeyValue(tPac, "Sexo Biológico:", sexoPac, fontBold, fontNormal);
            agregarParKeyValue(tPac, "Fecha de Nacimiento:", nacPac, fontBold, fontNormal);
            agregarParKeyValue(tPac, "Aseguradora (EPS):", eps, fontBold, fontNormal);
            agregarParKeyValue(tPac, "Fecha y Hora de Atención:", fechaAt + " " + horaAt, fontBold, fontNormal);
            document.add(tPac);

            // 2. Ingreso y Valoración de Triaje
            agregarSeccionHeader(document, "2. INGRESO Y VALORACIÓN DE TRIAJE", fontSub);
            PdfPTable tTri = new PdfPTable(2);
            tTri.setWidthPercentage(100);
            tTri.setSpacingAfter(8);

            String nivelTriaje = h.getNivelTriaje() != null ? h.getNivelTriaje() : "-";
            String dolorStr = h.getNivelDolorInicial() != null ? h.getNivelDolorInicial() + " / 10" : "-";
            String sintIniciales = h.getSintomasIniciales() != null ? h.getSintomasIniciales() : "-";

            agregarParKeyValue(tTri, "Nivel de Triaje Asignado:", nivelTriaje, fontBold, fontNormal);
            agregarParKeyValue(tTri, "Nivel de Dolor Reportado:", dolorStr, fontBold, fontNormal);
            agregarParKeyValue(tTri, "Síntomas Iniciales:", sintIniciales, fontBold, fontNormal);

            Triaje triajeObj = h.getAtencion() != null ? h.getAtencion().getTriaje() : null;
            if (triajeObj != null) {
                String fc = triajeObj.getFrecuenciaCardiaca() != null ? triajeObj.getFrecuenciaCardiaca() + " lpm" : "-";
                String fr = triajeObj.getFrecuenciaRespiratoria() != null ? triajeObj.getFrecuenciaRespiratoria() + " rpm" : "-";
                String pa = triajeObj.getPresionArterial() != null ? triajeObj.getPresionArterial() + " mmHg" : "-";
                String temp = triajeObj.getTemperatura() != null ? triajeObj.getTemperatura() + " °C" : "-";
                String spo2 = triajeObj.getSaturacionOxigeno() != null ? triajeObj.getSaturacionOxigeno() + " %" : "-";
                String cons = triajeObj.getEstadoConsciencia() != null ? triajeObj.getEstadoConsciencia() : "-";

                agregarParKeyValue(tTri, "Frecuencia Cardíaca (FC):", fc, fontBold, fontNormal);
                agregarParKeyValue(tTri, "Frecuencia Respiratoria (FR):", fr, fontBold, fontNormal);
                agregarParKeyValue(tTri, "Presión Arterial (PA):", pa, fontBold, fontNormal);
                agregarParKeyValue(tTri, "Temperatura Corporal:", temp, fontBold, fontNormal);
                agregarParKeyValue(tTri, "Saturación de Oxígeno (SpO2):", spo2, fontBold, fontNormal);
                agregarParKeyValue(tTri, "Estado de Consciencia:", cons, fontBold, fontNormal);
            }
            document.add(tTri);

            // 3. Valoración Clínica y Diagnóstico
            agregarSeccionHeader(document, "3. VALORACIÓN CLÍNICA Y DIAGNÓSTICO", fontSub);
            agregarBloqueTexto(document, "Motivo de Consulta:", h.getMotivoConsulta(), fontBold, fontNormal);
            agregarBloqueTexto(document, "Diagnóstico Principal:", h.getDiagnostico(), fontBold, fontNormal);
            agregarBloqueTexto(document, "Tratamiento y Procedimientos:", h.getTratamiento(), fontBold, fontNormal);
            agregarBloqueTexto(document, "Observaciones y Plan de Manejo:", h.getObservaciones(), fontBold, fontNormal);

            // 4. Profesional Tratante y Firma
            agregarSeccionHeader(document, "4. PROFESIONAL TRATANTE", fontSub);
            PdfPTable tMed = new PdfPTable(2);
            tMed.setWidthPercentage(100);
            tMed.setSpacingBefore(4);
            tMed.setSpacingAfter(24);

            Usuario med = h.getUsuario();
            String nombreMed = med != null && med.getNombreCompleto() != null && !med.getNombreCompleto().isBlank()
                    ? med.getNombreCompleto()
                    : (med != null ? med.getNombreUsuario() : "Médico de Guardia");
            String espMed = med != null && med.getEspecialidad() != null ? med.getEspecialidad() : "Medicina de Urgencias";

            agregarParKeyValue(tMed, "Médico Responsable:", nombreMed, fontBold, fontNormal);
            agregarParKeyValue(tMed, "Especialidad / Servicio:", espMed, fontBold, fontNormal);
            document.add(tMed);

            // Espacio formal para Firma y Sello
            Paragraph pFirma = new Paragraph();
            pFirma.setAlignment(Element.ALIGN_CENTER);
            pFirma.setKeepTogether(true);
            pFirma.add(new Paragraph("____________________________________________", fontNormal));
            pFirma.add(new Paragraph("Firma y Sello del Médico Tratante\nRegistro Médico Profesional • Servicio de Urgencias PRIORIX", fontNormal));
            document.add(pFirma);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar la historia clínica en PDF: " + e.getMessage(), e);
        }
    }

    private void agregarSeccionHeader(Document doc, String titulo, Font font) throws DocumentException {
        PdfPTable sectionTable = new PdfPTable(1);
        sectionTable.setWidthPercentage(100);
        sectionTable.setSpacingBefore(6);
        sectionTable.setSpacingAfter(4);

        PdfPCell cell = new PdfPCell(new Phrase(titulo, font));
        cell.setBackgroundColor(COLOR_BG_LIGHT);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidthLeft(3f);
        cell.setBorderColorLeft(COLOR_INSTITUCIONAL);
        cell.setPadding(4);
        cell.setPaddingLeft(6);
        sectionTable.addCell(cell);
        doc.add(sectionTable);
    }

    private void agregarBloqueTexto(Document doc, String label, String contenido, Font fontLabel, Font fontTexto) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(4);
        t.setKeepTogether(true);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(2);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", fontLabel));
        p.add(new Chunk(contenido != null && !contenido.isBlank() ? contenido : "No registrado.", fontTexto));
        cell.addElement(p);

        t.addCell(cell);
        doc.add(t);
    }

    private void agregarParKeyValue(PdfPTable table, String key, String value, Font fKey, Font fVal) {
        PdfPCell c1 = new PdfPCell(new Phrase(key, fKey));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(2.5f);
        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "-", fVal));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(2.5f);
        table.addCell(c1);
        table.addCell(c2);
    }

    private void agregarKpiCell(PdfPTable table, String label, String valor, Font fLabel, Font fValor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_BG_LIGHT);
        cell.setPadding(8);
        cell.setBorderColor(COLOR_BORDER);
        Paragraph p = new Paragraph(label, fLabel);
        p.setAlignment(Element.ALIGN_CENTER);
        Paragraph pv = new Paragraph(valor, fValor);
        pv.setAlignment(Element.ALIGN_CENTER);
        pv.setSpacingBefore(2);
        cell.addElement(p);
        cell.addElement(pv);
        table.addCell(cell);
    }

    private PdfPCell crearDatoCell(String texto, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setPadding(5);
        cell.setBorderColor(COLOR_BORDER);
        return cell;
    }

    /**
     * Evento de pie de página institucional con numeración de páginas precisa "Página X de Y".
     */
    private static class InstitucionalFooterEvent extends PdfPageEventHelper {
        private final String leyenda;
        private PdfTemplate totalPagesTemplate;
        private BaseFont baseFont;

        public InstitucionalFooterEvent(String leyenda) {
            this.leyenda = leyenda;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            try {
                totalPagesTemplate = writer.getDirectContent().createTemplate(30, 16);
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                totalPagesTemplate = null;
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();

            float left = document.leftMargin();
            float right = document.getPageSize().getWidth() - document.rightMargin();
            float y = document.bottomMargin() - 14;

            // Línea divisoria superior del pie de página
            cb.setLineWidth(0.6f);
            cb.setColorStroke(new Color(210, 226, 242));
            cb.moveTo(left, y + 10);
            cb.lineTo(right, y + 10);
            cb.stroke();

            // Texto de leyenda confidencial a la izquierda
            Phrase phraseLeyenda = new Phrase(leyenda, FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_MUTED));
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, phraseLeyenda, left, y, 0);

            // Numeración a la derecha
            String pageText = "Página " + writer.getPageNumber() + " de ";
            float len = baseFont != null ? baseFont.getWidthPointKerned(pageText, 7.5f) : 40;
            Phrase phrasePage = new Phrase(pageText, FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_MUTED));
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, phrasePage, right - 16, y, 0);

            if (totalPagesTemplate != null) {
                cb.addTemplate(totalPagesTemplate, right - 14, y);
            }
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            if (totalPagesTemplate != null && baseFont != null) {
                totalPagesTemplate.beginText();
                totalPagesTemplate.setFontAndSize(baseFont, 7.5f);
                totalPagesTemplate.setColorFill(COLOR_MUTED);
                totalPagesTemplate.setTextMatrix(0, 0);
                totalPagesTemplate.showText(String.valueOf(writer.getPageNumber() - 1));
                totalPagesTemplate.endText();
            }
        }
    }
}
