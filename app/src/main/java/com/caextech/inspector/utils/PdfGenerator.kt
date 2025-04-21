package com.caextech.inspector.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.caextech.inspector.data.entities.getCategoriaName
import com.caextech.inspector.data.relations.RespuestaConDetalles
import com.itextpdf.text.BaseColor
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.Image
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for generating PDF reports of No Conforme responses.
 */
object PdfGenerator {

    // Fonts
    private val TITLE_FONT = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD)
    private val HEADER_FONT = Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD)
    private val SUBHEADER_FONT = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD)
    private val NORMAL_FONT = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL)
    private val SMALL_FONT = Font(Font.FontFamily.HELVETICA, 8f, Font.NORMAL)

    /**
     * Generates a PDF document with the No Conforme responses.
     *
     * @param context The context for accessing resources
     * @param inspeccionId The ID of the inspection
     * @param noConformes List of No Conforme responses
     * @param outputFile The output PDF file
     */
    fun generatePdf(
        context: Context,
        inspeccionId: Long,
        noConformes: List<RespuestaConDetalles>,
        outputFile: File
    ) {
        // Create PDF document
        val document = Document()

        try {
            // Create PDF writer
            PdfWriter.getInstance(document, FileOutputStream(outputFile))

            // Open document
            document.open()

            // Add title
            val title = Paragraph("Reporte de Inspección - Items No Conformes", TITLE_FONT)
            title.alignment = Element.ALIGN_CENTER
            title.spacingAfter = 20f
            document.add(title)

            // Add inspection info
            addInspectionInfo(document, inspeccionId)

            // Add summary
            val summary = Paragraph("Total de ítems No Conformes: ${noConformes.size}", HEADER_FONT)
            summary.spacingBefore = 20f
            summary.spacingAfter = 10f
            document.add(summary)

            // Add No Conforme items
            if (noConformes.isNotEmpty()) {
                addNoConformeItems(context, document, noConformes)
            } else {
                document.add(Paragraph("No hay ítems No Conformes en esta inspección.", NORMAL_FONT))
            }

            // Add footer
            val footer = Paragraph("Documento generado el ${getCurrentDateTime()}", SMALL_FONT)
            footer.alignment = Element.ALIGN_CENTER
            footer.spacingBefore = 20f
            document.add(footer)

        } finally {
            // Close document
            document.close()
        }
    }

    /**
     * Generates a PDF document with both reception and delivery inspection data.
     * This version combines No Conforme items from reception and Rechazado items from delivery.
     *
     * @param context The context for accessing resources
     * @param entregaId The ID of the delivery inspection
     * @param recepcionId The ID of the reception inspection
     * @param noConformes List of No Conforme responses from reception
     * @param rechazados List of Rechazado responses from delivery
     * @param outputFile The output PDF file
     */
    fun generateDeliveryPdf(
        context: Context,
        entregaId: Long,
        recepcionId: Long,
        noConformes: List<RespuestaConDetalles>,
        rechazados: List<RespuestaConDetalles>,
        outputFile: File
    ) {
        // Create PDF document
        val document = Document()

        try {
            // Create PDF writer
            PdfWriter.getInstance(document, FileOutputStream(outputFile))

            // Open document
            document.open()

            // Add title
            val title = Paragraph("Reporte de Inspección de Entrega", TITLE_FONT)
            title.alignment = Element.ALIGN_CENTER
            title.spacingAfter = 20f
            document.add(title)

            // Add inspection info
            addDeliveryInspectionInfo(document, entregaId, recepcionId)

            // Add summary
            val summary = Paragraph("Resumen: ${noConformes.size} ítems No Conformes en Recepción, ${rechazados.size} ítems Rechazados en Entrega", HEADER_FONT)
            summary.spacingBefore = 20f
            summary.spacingAfter = 10f
            document.add(summary)

            // Add Reception section header
            val recepcionHeader = Paragraph("Ítems No Conformes en Inspección de Recepción", HEADER_FONT)
            recepcionHeader.spacingBefore = 20f
            recepcionHeader.spacingAfter = 10f
            document.add(recepcionHeader)

            // Add No Conforme items from reception
            if (noConformes.isNotEmpty()) {
                addNoConformeItems(context, document, noConformes)
            } else {
                document.add(Paragraph("No hay ítems No Conformes en la inspección de recepción.", NORMAL_FONT))
            }

            // Add Delivery section header
            val entregaHeader = Paragraph("Ítems Rechazados en Inspección de Entrega", HEADER_FONT)
            entregaHeader.spacingBefore = 20f
            entregaHeader.spacingAfter = 10f
            document.add(entregaHeader)

            // Add Rechazado items from delivery
            if (rechazados.isNotEmpty()) {
                addNoConformeItems(context, document, rechazados)
            } else {
                document.add(Paragraph("No hay ítems Rechazados en la inspección de entrega.", NORMAL_FONT))
            }

            // Add footer
            val footer = Paragraph("Documento generado el ${getCurrentDateTime()}", SMALL_FONT)
            footer.alignment = Element.ALIGN_CENTER
            footer.spacingBefore = 20f
            document.add(footer)

        } finally {
            // Close document
            document.close()
        }
    }

    /**
     * Adds delivery inspection information to the PDF.
     */
    private fun addDeliveryInspectionInfo(document: Document, entregaId: Long, recepcionId: Long) {
        val infoTable = PdfPTable(2)
        infoTable.widthPercentage = 100f

        // Add info rows
        addInfoRow(infoTable, "ID Inspección Recepción:", recepcionId.toString())
        addInfoRow(infoTable, "ID Inspección Entrega:", entregaId.toString())
        addInfoRow(infoTable, "Fecha:", getCurrentDateTime())

        document.add(infoTable)
    }
    /**
     * Adds inspection information to the PDF.
     */
    private fun addInspectionInfo(document: Document, inspeccionId: Long) {
        val infoTable = PdfPTable(2)
        infoTable.widthPercentage = 100f

        // Add info rows
        addInfoRow(infoTable, "ID Inspección:", inspeccionId.toString())
        addInfoRow(infoTable, "Fecha:", getCurrentDateTime())

        document.add(infoTable)
    }

    /**
     * Adds a row to the information table.
     */
    private fun addInfoRow(table: PdfPTable, label: String, value: String) {
        val labelCell = PdfPCell(Phrase(label, SUBHEADER_FONT))
        labelCell.border = Rectangle.NO_BORDER

        val valueCell = PdfPCell(Phrase(value, NORMAL_FONT))
        valueCell.border = Rectangle.NO_BORDER

        table.addCell(labelCell)
        table.addCell(valueCell)
    }

    /**
     * Adds No Conforme items to the PDF.
     */
    private fun addNoConformeItems(
        context: Context,
        document: Document,
        noConformes: List<RespuestaConDetalles>
    ) {
        // Group items by category
        val itemsByCategory = noConformes.groupBy { it.pregunta.getCategoriaName() }

        // Add each category and its items
        itemsByCategory.forEach { (categoria, items) ->
            // Add category header
            val categoryHeader = Paragraph(categoria, HEADER_FONT)
            categoryHeader.spacingBefore = 15f
            categoryHeader.spacingAfter = 5f
            document.add(categoryHeader)

            // Add items in this category
            items.forEach { respuesta ->
                addNoConformeItem(context, document, respuesta)
            }
        }
    }

    /**
     * Adds a single No Conforme item to the PDF.
     */
    private fun addNoConformeItem(
        context: Context,
        document: Document,
        respuesta: RespuestaConDetalles
    ) {
        // Create a table for the item
        val itemTable = PdfPTable(1)
        itemTable.widthPercentage = 100f

        // Question
        val questionCell = PdfPCell(Phrase(respuesta.pregunta.texto, SUBHEADER_FONT))
        questionCell.backgroundColor = BaseColor.LIGHT_GRAY
        questionCell.setPadding(8f)
        itemTable.addCell(questionCell)

        // Comments
        val commentsCell = PdfPCell()
        commentsCell.addElement(Paragraph("Comentarios:", SMALL_FONT))
        commentsCell.addElement(Paragraph(respuesta.respuesta.comentarios, NORMAL_FONT))
        commentsCell.setPadding(5f)
        itemTable.addCell(commentsCell)

        // Removed Action type and SAP ID section

        // Photos
        if (respuesta.tieneFotos()) {
            val photosCell = PdfPCell()
            photosCell.addElement(Paragraph("Evidencia fotográfica:", SMALL_FONT))

            // Add photos (up to 3)
            val numColumns = if (respuesta.fotos.isEmpty()) 1 else Math.min(respuesta.fotos.size, 3)
            val photoTable = PdfPTable(numColumns)
            photoTable.widthPercentage = 100f

            respuesta.fotos.take(3).forEach { foto ->
                try {
                    // Load photo
                    val bitmap = BitmapFactory.decodeFile(foto.rutaArchivo)

                    // Scale bitmap
                    val scaledBitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        300,  // width
                        225,  // height
                        true
                    )

                    // Convert to iText Image
                    val image = Image.getInstance(bitmapToByteArray(scaledBitmap))
                    image.scaleToFit(100f, 75f)

                    // Add to table
                    val cell = PdfPCell(image)
                    cell.horizontalAlignment = Element.ALIGN_CENTER
                    cell.verticalAlignment = Element.ALIGN_MIDDLE
                    cell.paddingTop = 5f
                    cell.paddingBottom = 5f
                    photoTable.addCell(cell)

                } catch (e: Exception) {
                    // If there's an error, add a placeholder cell
                    val cell = PdfPCell(Phrase("Error al cargar imagen", SMALL_FONT))
                    photoTable.addCell(cell)
                }
            }

            photosCell.addElement(photoTable)
            itemTable.addCell(photosCell)
        }

        // Add the item table to the document
        document.add(itemTable)

        // Add some space
        document.add(Paragraph(" "))
    }

    /**
     * Converts a bitmap to a byte array.
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }

    /**
     * Gets the current date and time formatted as a string.
     */
    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}