package sk.upjs.main.tools.csv;

import java.io.*;
import java.util.*;

import javax.swing.table.AbstractTableModel;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

@SuppressWarnings("serial")
public class CSVTableModel extends AbstractTableModel {

    /**
     * Zoznam riadkov tabulky
     */
    private List<Map<String, String>> rows;

    /**
     * Zoznam mien stlpcov
     */
    private List<String> columnNames;

    /**
     * Indikuje, ci tabulka je editovatelna
     */
    private boolean editable;

    /**
     * Vytvori prazdny model.
     */
    public CSVTableModel() {
	rows = new ArrayList<Map<String, String>>();
	columnNames = new ArrayList<String>();
    }

    /**
     * Vytvori inicializovany model.
     * 
     * @param columnNames
     * @param rows
     */
    public CSVTableModel(List<String> columnNames, List<Map<String, String>> rows) {
	this.columnNames = new ArrayList<String>(columnNames);
	this.rows = new ArrayList<Map<String, String>>();
	if (rows != null) {
	    for (Map<String, String> item : rows) {
		this.rows.add(new HashMap<String, String>(item));
	    }
	}
    }

    @Override
    public int getColumnCount() {
	return columnNames.size();
    }

    @Override
    public int getRowCount() {
	return rows.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
	if ((rowIndex < 0) || (rowIndex >= rows.size())) {
	    return null;
	}

	if ((columnIndex < 0) || (columnIndex >= columnNames.size())) {
	    return null;
	}

	return rows.get(rowIndex).get(columnNames.get(columnIndex));
    }

    @Override
    public String getColumnName(int column) {
	return columnNames.get(column);
    }

    /**
     * Vrati index stlpca so zadanym menom.
     * 
     * @param columnName
     *            meno hladaneho stlpca
     * @return index stplca alebo -1 ak stlpec s danym menom nie je definovany.
     */
    public int indexOfColumn(String columnName) {
	for (int i = 0; i < columnNames.size(); i++)
	    if (columnNames.get(i).equals(columnName))
		return i;

	return -1;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
	if ((rowIndex < 0) || (rowIndex >= rows.size())) {
	    return false;
	}

	if ((columnIndex < 0) || (columnIndex >= columnNames.size())) {
	    return false;
	}

	return editable;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
	if ((rowIndex < 0) || (rowIndex >= rows.size())) {
	    return;
	}

	if ((columnIndex < 0) || (columnIndex >= columnNames.size())) {
	    return;
	}

	if (aValue == null)
	    aValue = "";

	rows.get(rowIndex).put(columnNames.get(columnIndex), aValue.toString());
    }

    public boolean isEditable() {
	return editable;
    }

    public void setEditable(boolean editable) {
	this.editable = editable;
    }

    /**
     * Vrati objekt popisujuci dany riadok
     * 
     * @param rowIndex
     * @return
     */
    public Map<String, String> getRow(int rowIndex) {
	if ((rowIndex < 0) || (rowIndex >= rows.size())) {
	    return null;
	}

	return new HashMap<String, String>(rows.get(rowIndex));
    }

    /**
     * Vlozi novy zaznam na zadanu poziciu
     * 
     * @param rowIndex
     * @param record
     */
    public void insertNewRow(int rowIndex, Map<String, String> record) {
	if (record == null)
	    return;

	if (rowIndex < 0)
	    rowIndex = 0;

	if (rowIndex >= rows.size())
	    rowIndex = rows.size();

	rows.add(rowIndex, new HashMap<String, String>(record));
	fireTableDataChanged();
    }

    /**
     * Odstrani riadok na zadanej pozicii.
     * 
     * @param rowIndex
     */
    public void removeRow(int rowIndex) {
	if ((rowIndex < 0) || (rowIndex >= rows.size()))
	    return;

	rows.remove(rowIndex);
	fireTableDataChanged();
    }

    /**
     * Ulozi obsah csv tabulky do suboru.
     * 
     * @param file
     * @throws IOException
     */
    public void saveToFile(File file) throws IOException {
	CSVWriter writer = null;
	try {
	    writer = new CSVWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")), ';');
	    // Zapiseme hlavicku
	    String[] rowItems = new String[0];
	    rowItems = columnNames.toArray(rowItems);
	    writer.writeNext(rowItems);

	    // Zapiseme jednotlive zaznamy
	    for (Map<String, String> row : rows) {
		for (int i = 0; i < rowItems.length; i++)
		    rowItems[i] = row.get(columnNames.get(i));

		writer.writeNext(rowItems);
	    }
	} catch (Exception e) {
	    throw e;
	} finally {
	    if (writer != null)
		writer.close();
	}
    }

    /**
     * Nacita a vytvori model podla csv suboru, v ktorom prvy riadok obsahuje
     * nazvy stplcov
     * 
     * @param csvFile
     *            csv subor
     * @return
     * @throws IOException
     */
    public static CSVTableModel loadFromFile(File csvFile) throws IOException {
	CSVReader reader = null;
	ArrayList<String> columns = new ArrayList<String>();
	List<Map<String, String>> rows = new ArrayList<Map<String, String>>();

	try {
	    reader = new CSVReader(new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), "UTF-8")),
		    ';');

	    // Nacitame hlavicku
	    String[] headerLine = reader.readNext();
	    if (headerLine != null) {
		for (int i = 0; i < headerLine.length; i++)
		    columns.add(headerLine[i]);
	    }

	    // Nacitame riadky
	    if (columns.size() != 0) {
		String[] nextLine;
		while ((nextLine = reader.readNext()) != null) {
		    Map<String, String> row = new HashMap<String, String>();
		    int definedCols = Math.min(nextLine.length, columns.size());
		    for (int i = 0; i < definedCols; i++)
			row.put(columns.get(i), nextLine[i]);
		    rows.add(row);
		}
	    }
	} catch (Exception e) {
	    throw e;
	} finally {
	    if (reader != null)
		reader.close();
	}

	// Vytvorime table model
	return new CSVTableModel(columns, rows);
    }
}
