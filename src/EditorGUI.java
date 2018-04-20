
import javax.swing.*;
import javax.swing.Action;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
import java.io.*;


public class EditorGUI extends JFrame implements ActionListener, DocumentListener, WindowListener {

	private static final Object[] fileExistOptions = {"Tak", "Wybierz inny", "Przerwij"};
	private static final Object[] changeFileOptions = {"Tak", "Nie zapisuj", "Anuluj"};
	private static final String fileExistMsg = "Wybrano istniejący plik\nCzy chcesz go nadpisać?";
	private static final String fileCloseMsg = "Czy chcesz zapisać zmiany\n     przed zamknięciem?";
	private static final String newFileMsg = "Zapisać wprowadzone zmiany?";
	private static final String fileOpenMsg = "Zapisać zmiany przed otwarciem?";
	private static final int[] tabSizes = {8,9,10,11,12,14,16,18,20,24,28,32};
	private static final int defaultSize = 11;
	private static final Color editorEnabled = new Color(84, 88, 90);
	private static final Color editorDisabled = new Color(120,120,120);
	private static final Color buttonPressed = new Color(242,242,242);
	private static final Color buttonNPressed = new Color(121,174,120);

	private final Action newFileAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			newFileFunc();
		}
	};
	private final Action openFileAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			openFileFunc();
		}
	};
	private final Action saveFileAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			saveFileFunc();
		}
	};
	private final Action closeFileAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			closeFileFunc();
		}
	};

	private JPanel rootPanel;
	private JToolBar buttonsMenuBar;
	private JButton openField;
	private JButton closeField;
	private JButton newField;
	private JButton saveField;
	private JToolBar.Separator menuSeparator;
	private JComboBox fontSize;
	private JScrollPane resizeScroll;
	private JTextArea editorField;
	private JButton boldButton;
	private JButton italicButton;
	private JLabel cursorPosLabel;
	private JFileChooser fc;

	private File savedFilePath = null;
	private boolean fileSaved = false;
	private boolean textEdited = false;
	private boolean defaultSettings;
	private EditorStartData editorPrefData;


	EditorGUI() {
		setTitle("Bez nazwy* - MatiNote");
		ImageIcon img = new ImageIcon("mati_note_logo.png");
		setIconImage(img.getImage());

		//wczytywanie ostatnio uzywanych ustawien z pliku jesli istnieje
		//(wymiary okna, sciezka do pliku, wielkosci i styl fontu)
		editorPrefData = new EditorStartData();
		if ((new File("editor_prefs.bin")).exists()) {
			defaultSettings = true;
			try {
				ObjectInputStream dataIn = new ObjectInputStream(new FileInputStream("editor_prefs.bin"));
				editorPrefData = (EditorStartData)dataIn.readObject();
				defaultSettings = false;
			} catch (IOException ioex) {
				System.out.println("Blad odczytywyania ustawień\nZostaną wczytane wartosci domyslne");
				editorPrefData = new EditorStartData();
			} catch (ClassNotFoundException cnfex) {
				System.out.println("Blad odczytywania, nie rozpoznano klasy zrodlowej");
				editorPrefData = new EditorStartData();
			}
		} else {
			System.out.println("Nie znaleziono pliku z ustawieniami");
			defaultSettings = true;
		}

		initComponents();
		addListneres();
		pack();
	}

	private void initComponents() {
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		fc = new JFileChooser(System.getProperty("user.home//documents"));
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files (*.txt)", "txt");
		fc.setFileFilter(filter);

		int userFontSize;
		Dimension prefDimension;
		//int userFontStyle;
		if (defaultSettings) {
			prefDimension = new Dimension(500,360);
			userFontSize = defaultSize;
			//userFontStyle = 0;
		} else {
			prefDimension = editorPrefData.prefSize;
			userFontSize = editorPrefData.fontSize;
			//userFontStyle = editorPrefData.fontStyle;
			if (editorPrefData.dirPath.exists())
				fc.setCurrentDirectory(editorPrefData.dirPath);
		}


		setMinimumSize(new Dimension(240, 71));
		setPreferredSize(prefDimension);



		centerFrame();
		setContentPane(rootPanel);
		editorField.requestFocus();

		fontSize.setMaximumSize(new Dimension(44,30));
		fontSize.setPreferredSize(new Dimension(44,30));
		fontSize.setMaximumRowCount(5);
		for (int i = 0; i < tabSizes.length; i++) {
			fontSize.addItem(tabSizes[i]);
			if(tabSizes[i] == userFontSize) {
				fontSize.setSelectedIndex(i);
				editorField.setFont(new Font("Consolas", Font.PLAIN, (int)Math.round(tabSizes[i]*1.34)));
				editorPrefData.fontSize = tabSizes[i];
			}
		}
		cursorPosLabel.setText("Lin 1,  pos 1");

		SwingUtilities.updateComponentTreeUI(this);
		setVisible(true);
	}

	private void addListneres() {
		openField.addActionListener(this);
		closeField.addActionListener(this);
		saveField.addActionListener(this);
		newField.addActionListener(this);
		boldButton.addActionListener(this);
		italicButton.addActionListener(this);

		//obsluga zmiany wielkosci czcionki
		fontSize.addItemListener(event -> {
			if (event.getStateChange() == ItemEvent.SELECTED) {
				int size = Integer.parseInt(event.getItem().toString());
				Font font = editorField.getFont();
				//SwingUtilities.invokeLater(
						  //new FontChanger(font.getName(), font.getStyle(), (int)Math.round(size * 1.34), null));
				new Thread(new FontChanger(font.getName(), font.getStyle(), (int)Math.round(size * 1.34), null)).start();
				editorPrefData.fontSize = size;
			}
		});

		//oblusga zmiany polozenia kursora
		editorField.addCaretListener(event -> {
			//SwingUtilities.invokeLater(new CursorPosition());
			new Thread(new CursorPosition(event.getDot())).start();
		});

		editorField.getDocument().addDocumentListener(this);
		addWindowListener(this);

		InputMap im = editorField.getInputMap();
		ActionMap am = editorField.getActionMap();

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK), "newFile");
		am.put("newFile", newFileAction);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK), "openFile");
		am.put("openFile", openFileAction);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK), "saveFile");
		am.put("saveFile", saveFileAction);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK), "closeFile");
		am.put("closeFile", closeFileAction);

		rootPanel.getActionMap().put("newFile", newFileAction);
		rootPanel.getActionMap().put("openFile", openFileAction);

		editorField.requestFocus();
	}


	//--- IMPLEMENTACJA METOD Z INTERFEJSOW NASLUCHUJACYCH ZMIAN W PANELU TEXTAREA START ------//
	public void changedUpdate(DocumentEvent ev) {
		if(!textEdited)
			textEdited = true;
	}

	public void removeUpdate(DocumentEvent ev) {
		if(!textEdited)
			textEdited = true;
	}

	public void insertUpdate(DocumentEvent ev) {
		if(!textEdited)
			textEdited = true;
	}
	//--- IMPLEMENTACJA METOD Z INTERFEJSOW NASLUCHUJACYCH ZMIAN W PANELU TEXTAREA KONIEC ------//

	//-------------- METODY NASLUCHUJACE ZMIANY STANU OKNA APLIKACJI START --------------------//
	//obsluga zamkniecia (krzyzyk lub Alt+F4)
	public void windowClosing(WindowEvent we) {
		int nOption;
		//System.out.println("Zamykanie");
		//System.out.println(editorField.getDocument().getLength());

		if (textEdited && ((editorField.getDocument().getLength() != 0) || fileSaved)) {
			nOption = JOptionPane.showOptionDialog(rootPanel, fileCloseMsg, "Zamykanie programu", JOptionPane.YES_NO_CANCEL_OPTION,
					  JOptionPane.QUESTION_MESSAGE, null, changeFileOptions, changeFileOptions[0]);

			if (nOption == JOptionPane.CANCEL_OPTION || nOption == JOptionPane.CLOSED_OPTION)
				return;
			else if (nOption == 0) {
				//SwingUtilities.invokeLater(new HandleEditorField(4, true));
				new Thread(new HandleEditorField(4, true)).start();
				return;
			}
		}
		//SwingUtilities.invokeLater(new HandleEditorField(4, false));
		new Thread(new HandleEditorField(4, false)).start();
	}

	public void windowClosed(WindowEvent we) {}

	public void windowDeiconified(WindowEvent we) {}

	public void windowIconified(WindowEvent we) {}

	public void windowOpened(WindowEvent we) {}

	public void windowActivated(WindowEvent e) {}

	public void windowDeactivated(WindowEvent e) {}
	//-------------- METODY NASLUCHUJACE ZMIANY STANU OKNA APLIKACJI KONIEC --------------------//

	//obsluga klikalnych przyciskow
	public void actionPerformed(ActionEvent e) {
		//obsluga przycisku otwierania pliku
		if (e.getSource() == newField) {
			newFileFunc();
		}

		//obsluga przycisku zamykania pliku
		else if (e.getSource() == openField) {
			openFileFunc();
		}

		//obsluga przycysku zapisywania pliku
		else if (e.getSource() == saveField) {
			saveFileFunc();
		}

		//obsluga przycisku nowego pliku
		else if (e.getSource() == closeField) {
			closeFileFunc();
		}

		//obsluga klawisza pogrubiania czcionki
		else if (e.getSource() == boldButton) {
			int fStyle;
			Font font = editorField.getFont();
			if (boldButton.isSelected()) {
				if (font.getStyle() == Font.BOLD)
				fStyle = 0;
				else
				fStyle = 2;
			} else {
				if (font.getStyle() == Font.PLAIN)
					fStyle = 1;
				else
					fStyle = 3;
			}

			//SwingUtilities.invokeLater(new FontChanger(font.getName(), fStyle, font.getSize(), boldButton));
			//System.out.println(SwingUtilities.isEventDispatchThread());
			new Thread(new FontChanger(font.getName(), fStyle, font.getSize(), boldButton)).start();
		}

		//obsluga klawisza pochylania czcionki
		else if (e.getSource() == italicButton) {
			int fStyle;
			Font font = editorField.getFont();
			if (italicButton.isSelected()) {
				if (font.getStyle() == Font.ITALIC)
					fStyle = 0;
				else
					fStyle = 1;
			} else {
				if (font.getStyle() == Font.PLAIN)
					fStyle = 2;
				else
					fStyle = 3;
			}
			//SwingUtilities.invokeLater(new FontChanger(font.getName(), fStyle, font.getSize(), italicButton));
			new Thread(new FontChanger(font.getName(), fStyle, font.getSize(), italicButton)).start();
		}
	}

	//obsluga zlecenia zmiany wyswietlania informacji o polozeniu kursora
	private class CursorPosition implements Runnable {
		private int cursorOffset;

		CursorPosition(int cursorOffset) {
			this.cursorOffset = cursorOffset;
		}

		@Override
		public void run() {
			int lineNr = 1;
			int lineStart = 1;
			try {
				lineNr = editorField.getLineOfOffset(cursorOffset);
				lineStart = editorField.getLineStartOffset(lineNr);
			} catch (BadLocationException lex) {
				System.out.println("nieprawidlowy offset");
				lex.printStackTrace();
			}
			cursorPosLabel.setText("Lin " + (lineNr+1) + ",  pos " + (cursorOffset-lineStart+1));
		}
	}

	//obsluga zlecenia zmiany czcionki
	private class FontChanger implements Runnable {
		int fStyle, fSize;
		String fName;
		JButton buttonState;

		FontChanger(String fName, int fStyle, int fSize, JButton bState) {
			this.fName = fName;
			this.fStyle = fStyle;
			this.fSize = fSize;
			buttonState = bState;
		}

		@Override
		public void run() {
			if (buttonState != null) {
				if (buttonState.isSelected()) {
					buttonState.setSelected(false);
					buttonState.setForeground(buttonNPressed);
				} else {
					buttonState.setSelected(true);
					buttonState.setForeground(buttonPressed);
				}
			}
			editorField.setFont(new Font(fName, fStyle, fSize));
			editorPrefData.fontStyle = fStyle;
		}
	}

	//obsluga zlecenia zapisu do pliku
	private class HandleEditorField implements Runnable {
		File path;
		int operationId;
		boolean doSaveFile;

		HandleEditorField(int operationId, boolean saveFile) {
			this.operationId = operationId;
			doSaveFile = saveFile;
		}

		@Override
		public void run() {
			//zapisywanie pliku jezeli wywolano HandleEditorField(x, true)
			if (doSaveFile) {
				//wywolanie okna zapisywania pliku jezeli jeszcze nie zapisany
				if (!fileSaved) {
					int nOption;
					int returnVal = fc.showSaveDialog(rootPanel);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
						path = fc.getSelectedFile();
						if (path.isDirectory()) {
							JOptionPane.showMessageDialog(rootPanel, "Error, directory was selected");
							return;
						}

						//wyswietlenie monitu o mozliwym nadpisaniu wskazanego pliku
						while (path.exists()) {
							nOption = JOptionPane.showOptionDialog(rootPanel, fileExistMsg, "Plik istnieje", JOptionPane.YES_NO_CANCEL_OPTION,
									  JOptionPane.WARNING_MESSAGE, null, fileExistOptions, fileExistOptions[2]);
							if (nOption == 0)
								break;
							else if (nOption == 2)
								return;
							if (fc.showSaveDialog(rootPanel) == JFileChooser.CANCEL_OPTION)
								return;
							path = fc.getSelectedFile();
						}

						if (path.exists() && !path.canWrite()) {
							JOptionPane.showMessageDialog(rootPanel, "Błąd. Plik zabezpieczony przed zapisem");
							return;
						}

						String fileName = path.getName();
						if (fileName.length() < 4 ||
								  !fileName.substring(fileName.length() - 4, fileName.length()).equalsIgnoreCase(".txt"))
						{
							fileName = fileName.concat(".txt");
							File tmpPath = new File(path.getParentFile(), fileName);
							if (!tmpPath.exists())
								path = tmpPath;
						}
						savedFilePath = path;
					} else
						return;
				}

				//fragment wlasciwy zapisywania do pliku
				BufferedWriter outputFile = null;
				try {
					outputFile = new BufferedWriter(new FileWriter(savedFilePath));
					editorField.write(outputFile);
				} catch (IOException ioex) {
					ioex.printStackTrace();
				} finally {
					if (outputFile != null)
						try {
							outputFile.close();
							textEdited = false;
						} catch (IOException ioex) {
							ioex.printStackTrace();
							System.out.println("Fatal error");
							JOptionPane.showMessageDialog(rootPanel, "FATAL ERROR",
									  "This should never happen", JOptionPane.ERROR_MESSAGE);
						}
				}
			}
			//zmiana tytulu okna przy pierwszym zapisaniu pliku
			if (operationId == 0) {
				if (!fileSaved) {
					fileSaved = true;
					setTitle(savedFilePath.getName() + " - MatiNote");
				}
			}
			//obsluga polecenia zamkniecia pliku w edytorze
			else if (operationId == 1) {

				editorField.setText("");
				editorField.setEnabled(false);
				editorField.setBackground(editorDisabled);
				setTitle("NOFILE in MatiNote");
				saveField.setEnabled(false);
				closeField.setEnabled(false);
				fileSaved = false;
				textEdited = false;

				//dodanie mapowania skrotow Ctrl+N i Ctrl+O dla rootPanel, poniewaz
				//po zablokowaniu editorField, nie przyjmuje on wejscia z klawiatury i myszy
				rootPanel.requestFocus();
				rootPanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK), "newFile");
				rootPanel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK), "openFile");
			}
			//obsluga tworzenia nowej przestrzeni roboczej
			else if (operationId == 2) {
				//usuwanie map Ctrl+N i Ctrl+O dla rootPanel przed wznowieniem okna edytora
				if (!editorField.isEnabled()) {
					rootPanel.getInputMap().remove(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
					rootPanel.getInputMap().remove(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
				}

				setTitle("Bez nazwy* - MatiNote");
				if (!closeField.isEnabled()) {
					resumeEditor();
				} else
					editorField.setText("");
				fileSaved = false;
				textEdited = false;
			}
			//wywolanie otwierania pliku po zapisaniu poprzednich danych
			else if (operationId == 3) {
				//SwingUtilities.invokeLater(new HandleOpenFile());
				new Thread(new HandleOpenFile()).start();
			}
			//wyjscie z programu po zapisaniu danych
			else if (operationId == 4) {
				//tworzenie klasy z ustawieniami
				editorPrefData.dirPath = fc.getCurrentDirectory();
				editorPrefData.prefSize = EditorGUI.super.getSize();

				//serializacja ustawien do pliku "editor_prefs.bin"
				ObjectOutputStream dataOut;
				try {
					dataOut = new ObjectOutputStream(new FileOutputStream("editor_prefs.bin"));
					dataOut.writeObject(editorPrefData);
					dataOut.close();
				} catch (FileNotFoundException fnfex) {
					JOptionPane.showMessageDialog(rootPanel, "Błąd podczas tworzenia pliku ustawień");
				} catch (IOException ioex) {
					JOptionPane.showMessageDialog(rootPanel, "Błąd zapisywania ustawień do pliku");
				}

				//zakonczenie programu
				System.exit(0);
			}
		}
	}

	//obsluga zlecenia wczytywania z pliku
	private class HandleOpenFile implements Runnable {
		File path;
		boolean readOk;
		HandleOpenFile() {
			readOk = false;
		}

		@Override
		public void run() {

			int returnVal = fc.showOpenDialog(rootPanel);
			if (returnVal == JFileChooser.APPROVE_OPTION) {

				//otwieranie pliku dla wskazanej sciezki
				path = fc.getSelectedFile();
				//sprawdzenie czy wskazany plik istnieje
				while (!path.exists()) {
					JOptionPane.showMessageDialog(rootPanel, "Nie ma takiego pliku\nSprawdź nazwę\ni spróbuj ponownie",
							  "Błąd wczytywania", JOptionPane.WARNING_MESSAGE);
					returnVal = fc.showOpenDialog(rootPanel);
					if (returnVal == JFileChooser.CANCEL_OPTION)
						return;
					else
						path = fc.getSelectedFile();
				}

				//usuwanie map Ctrl+N i Ctrl+O dla rootPanel przed wznowieniem okna edytora
				if (!editorField.isEnabled()) {
					rootPanel.getInputMap().remove(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
					rootPanel.getInputMap().remove(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
				}

				//sprawdzanie czy wskazany plik jest w formacie .txt
				String fileName = path.getName();
				if (fileName.length() < 4 ||
						  !fileName.substring(fileName.length() - 4, fileName.length()).equalsIgnoreCase(".txt"))
				{
					JOptionPane.showMessageDialog(rootPanel, "Niepoprawny format pliku,\nodczyt niemożliwy",
							  "Błąd wczytywania", JOptionPane.WARNING_MESSAGE);
					return;
				} else if (!path.canRead()) {
					JOptionPane.showMessageDialog(rootPanel, "Nie można odczytać pliku,\nodmowa dostępu",
							  "Błąd wczytywania", JOptionPane.WARNING_MESSAGE);
					return;
				}

				//wnawianie obszaru edytora jezeli zostal wczesniej zablokowany
				if (!closeField.isEnabled()) {
					resumeEditor();
				}

				//odczytanie danych z pliku do obszaru edytora
				BufferedReader readFile = null;
				try {
					readFile = new BufferedReader(new FileReader(path));
					editorField.read(readFile,".txt");
					readOk = true;
				} catch (IOException rex) {
					rex.printStackTrace();
				}

				if (readFile != null) {
					try {
						readFile.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				//reaktywacja nasluchiwania eventow edytora dla wczytanych danych
				//koncowe ustawienie edytora zalezne od powodzenia wczytywania,
				editorField.getDocument().removeDocumentListener(EditorGUI.this);
				editorField.getDocument().addDocumentListener(EditorGUI.this);
				if (readOk) {
					setTitle(path.getName() + " - MatiNote");
					fileSaved = true;
					savedFilePath = path;
				} else {
					setTitle("Bez nazwy* - MatiNote");
					editorField.setText("");
					fileSaved = false;
					JOptionPane.showMessageDialog(rootPanel,
							  "Wystąpił błąd,\nodczytywanie nie powiodło się", "Błąd odczytu", JOptionPane.ERROR_MESSAGE);
				}
				textEdited = false;
			}
		}
	}

	//wnawianie obszaru edytora jezeli zostal wczesniej zablokowany
	private void resumeEditor() {
		saveField.setEnabled(true);
		closeField.setEnabled(true);
		editorField.setBackground(editorEnabled);
		editorField.setEnabled(true);
		editorField.requestFocus();
	}

	//funkcja realizujaca dzialania dla przycisku newField i skrotu Ctrl+N
	private void newFileFunc() {
		if (textEdited && ((editorField.getDocument().getLength() != 0) || fileSaved)) {
			//System.out.println("Tworzenie czystego pliku");
			int nOption = JOptionPane.showOptionDialog(rootPanel, newFileMsg, "Zapisywanie pliku",
					  JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
					  changeFileOptions, changeFileOptions[0]);

			if (nOption == JOptionPane.CANCEL_OPTION || nOption == JOptionPane.CLOSED_OPTION)
				return;
			else if (nOption == 0) {
				//SwingUtilities.invokeLater(new HandleEditorField(2, true));
				new Thread(new HandleEditorField(2, true)).start();
				return;
			}
		}
		if (fileSaved || !closeField.isEnabled() || editorField.getDocument().getLength() != 0)
			//SwingUtilities.invokeLater(new HandleEditorField(2, false));
			new Thread(new HandleEditorField(2, false)).start();
	}

	//funkcja realizujaca dzialania dla przycisku openField i skrotu Ctrl+O
	private void openFileFunc() {


		if (textEdited && ((editorField.getDocument().getLength() != 0) || fileSaved)) {
			int nOption = JOptionPane.showOptionDialog(rootPanel, fileOpenMsg, "Wprowadzono zmiany",
					  JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
					  changeFileOptions, changeFileOptions[0]);

			if (nOption == JOptionPane.CANCEL_OPTION || nOption == JOptionPane.CLOSED_OPTION)
				return;
			else if (nOption == 0) {
				//SwingUtilities.invokeLater(new HandleEditorField(3, true));
				new Thread(new HandleEditorField(3, true)).start();
				return;
			}
		}
		//SwingUtilities.invokeLater(new HandleOpenFile());
		new Thread(new HandleOpenFile()).start();
	}

	//funkcja realizujaca dzialania dla przycisku saveField i skrotu Ctrl+S
	private void saveFileFunc() {
		if (textEdited || !fileSaved) {
			//System.out.println("Zapisywanie");
			//SwingUtilities.invokeLater(new HandleEditorField(0, true));
			new Thread(new HandleEditorField(0, true)).start();
		}
	}

	//funkcja realizujaca dzialania dla przycisku closeField i skrotu Ctrl+W
	private void closeFileFunc() {
		int nOption;
		//System.out.println("Zamykanie");
		if (textEdited && ((editorField.getDocument().getLength() != 0) || fileSaved)) {
			nOption = JOptionPane.showOptionDialog(rootPanel, fileCloseMsg, "Zamykanie pliku", JOptionPane.YES_NO_CANCEL_OPTION,
					  JOptionPane.QUESTION_MESSAGE, null, changeFileOptions, changeFileOptions[0]);

			if (nOption == JOptionPane.CANCEL_OPTION || nOption == JOptionPane.CLOSED_OPTION)
				return;
			else if (nOption == 0) {
				//SwingUtilities.invokeLater(new HandleEditorField(1, true));
				new Thread(new HandleEditorField(1, true)).start();
				return;
			}
		}
		//SwingUtilities.invokeLater(new HandleEditorField(1, false));
		new Thread(new HandleEditorField(1, false)).start();
	}

	//ustawianie okna na srodku ekranu
	private void centerFrame() {
		Dimension windowSize = getPreferredSize();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Point centerPoint = ge.getCenterPoint();
		int dx = centerPoint.x - windowSize.width/2;
		int dy = centerPoint.y - windowSize.height/2;
		setLocation(dx, dy);
	}

	static class EditorStartData implements Serializable {
		Dimension prefSize;
		File dirPath;
		int fontSize;
		int fontStyle;
	}

}
