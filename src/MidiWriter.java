import java.io.*;
import javax.sound.midi.*;

public class MidiWriter {
	
	private Sequence sequence;
	
	static final String[] notes = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
	
	/**
	 * Creates a new {MidiWriter} instance.
	 * 
	 * @param divisionType the timing division type of the sequence
	 * @param resolution the timing resolution of the sequence
	 * 
	 * @throws InvalidMidiDataException 
	 */
	public MidiWriter(float divisionType, int resolution) throws InvalidMidiDataException {
		this.sequence = new Sequence(divisionType, resolution);
	}
	
	/**
	 * Adds a new {@code Track} to the sequence.
	 */
	public void createTrack() {
		sequence.createTrack();
	}
	
	/**
	 * Deletes the target track.
	 * 
	 * @param trackNumber the target track
	 */
	public void deleteTrack(int trackNumber) {
	    if(trackNumber > 0 && trackNumber < sequence.getTracks().length) {
	        Track track = sequence.getTracks()[trackNumber];
	        sequence.deleteTrack(track);
	    }
	}
	
	/**
	 * Adds a {@code MidiEvent} to the target track.
	 * 
	 * @param trackNumber the target track
	 * @param event the {@code MidiEvent} that will be added to the track
	 */
	public void addEvent(int trackNumber, MidiEvent event) {
		Track track = sequence.getTracks()[trackNumber];
		track.add(event);
	}
	
	/**
	 * Creates a new {@code MidiEvent} and adds it to the target track.
	 * 
	 * @param trackNumber the target track
	 * @param message the {@code MidiMessage} that will be added to the track
	 * @param timestamp the timestamp of the {@code MidiMessage}, in ticks
	 */
	public void addMessage(int trackNumber, MidiMessage message, long timestamp) {
		MidiEvent event = new MidiEvent(message, timestamp);
		addEvent(trackNumber, event);
	}
	
	/**
	 * Returns the length of a quarter note based on the current timing resolution.
	 * 
	 * @return the length of a quarter note in the sequence, in ticks
	 */
	public int getQuarterNoteLength() {
	    return sequence.getResolution();
	}
	
	/**
	 * Enables the General MIDI Sound set for the target track.
	 * 
	 * @param trackNumber the target track.
	 * 
	 * @throws InvalidMidiDataException
	 */
	public void enableGeneralMidiSoundset(int trackNumber) throws InvalidMidiDataException {
	    byte[] data = {(byte) 0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte) 0xF7};
	    SysexMessage message = new SysexMessage(data, 6);
	    addMessage(trackNumber, message, 0);
	}
	
	/**
	 * Sets the name of the target track.
	 * 
	 * @param trackNumber the target track
	 * @param name the name to be set
	 * 
	 * @throws InvalidMidiDataException 
	 */
	public void setTrackName(int trackNumber, String name) throws InvalidMidiDataException {
		byte[] encodedName = name.getBytes();
		MetaMessage message;
		message = new MetaMessage(0x03, encodedName, encodedName.length);
		addMessage(trackNumber, message, 0);

	}
	
	/**
	 * Sets OMNI mode of the target track to either on or off.
	 * 
	 * @param trackNumber the target track
	 * @param setting the OMNI setting; true if on and false if off
	 * 
	 * @throws InvalidMidiDataException 
	 */
	public void setOmni(int trackNumber, boolean setting) throws InvalidMidiDataException {
		int omniSetting;
		if(setting) {
			omniSetting = 0x7D; //message for OMNI on
		} else {
			omniSetting = 0x7C; //message for OMNI off
		}
	
		ShortMessage message = new ShortMessage(ShortMessage.CONTROL_CHANGE, omniSetting, 0x00);
		addMessage(trackNumber, message, 0);
	}
	
	/**
	 * Sets the target track to either POLY or MONO.
	 * 
	 * @param trackNumber the target track
	 * @param setting true if POLY, false if MONO
	 * 
	 * @throws InvalidMidiDataException 
	 */
	public void setPoly(int trackNumber, boolean setting) throws InvalidMidiDataException {
		int polySetting;
		if(setting) {
			polySetting = 0x7F; //message for POLY on
		} else {
			polySetting = 0x7E; //message for MONO on
		}
		
		ShortMessage message = new ShortMessage(ShortMessage.CONTROL_CHANGE, polySetting, 0x00);
		addMessage(trackNumber, message, 0);
	}
	
	/**
	 * Sets the instrument of the target track.
	 * 
	 * @param trackNumber the target track
	 * @param instrument the number of the instrument as defined by MIDI specifications
	 * 
	 * @throws InvalidMidiDataException 
	 */
	public void setInstrument(int trackNumber, int instrument) throws InvalidMidiDataException {
		ShortMessage message = new ShortMessage(ShortMessage.PROGRAM_CHANGE, instrument, 0x00);
		addMessage(trackNumber, message, 0);
	}
	
	/**
	 * Sets the BPM (beats per minute) of the target track at the given timestamp
	 * 
	 * @param trackNumber the target track
	 * @param beatsPerMinute the desired tempo, in beats per minute
	 * @param timestamp the timestamp of the BPM change
	 * 
	 * @throws InvalidMidiDataException
	 */
	public void setBPM(int trackNumber, int beatsPerMinute, long timestamp)
			throws InvalidMidiDataException {
	      byte[] encodedTempo = intToBytes(60000000 / beatsPerMinute);
	      MetaMessage message = new MetaMessage(0x51, encodedTempo, encodedTempo.length);
	      addMessage(trackNumber, message, timestamp);
	}
	
	//converts an integer into an array of three bytes; assumes no overflow
	private byte[] intToBytes(int num) {
		byte[] data = new byte[3];
		data[0] = (byte)(num >> 16);
		data[1] = (byte)(num >> 8);
		data[2] = (byte)(num);
		return data;
	}
	
	/**
	 * Converts {@code String} representation of note (ex. A#5) to MIDI number (ex. 70)
	 * @param noteString the note
	 * 
	 * @return the MIDI number of the note
	 */
	public static int noteStringToInt(String noteString) {
		noteString = noteString.toUpperCase();
		String noteLetter = "";
		if(noteString.length() == 2) {
			noteLetter = noteString.substring(0, 1);
		} else if(noteString.length() == 3) {
			noteLetter = noteString.substring(0, 2);
		} else {
			throw new IllegalArgumentException("Unrecognized note");
		}
		//get note part of string

		int octave = Integer.parseInt(noteString.substring
				(noteString.length() - 1, noteString.length()));
		//get last character of string for octave number
		
		int noteNum = 0;
		while(noteNum < notes.length && !notes[noteNum].equals(noteLetter)) {
			noteNum++;
			//search for index of noteString in static notes array
		}
		if(noteNum > notes.length) {
		    throw new IllegalArgumentException("Unrecognized note");
		}
		
		return octave * 12 + noteNum;
	}
	
	/**
	 * Converts MIDI number of note (ex. 70) to {@code String} representation of note (ex. A#5)
	 * 
	 * @param noteNumber the MIDI number of the note
	 * 
	 * @return the {@code String} representation of the note
	 */
	public static String noteIntToString(int noteNumber) {
	    if(noteNumber < 0 || noteNumber > 127) {
	        throw new IllegalArgumentException("Note out of range (0-127)");
	    }
	    
	    return notes[noteNumber % 12] + (noteNumber / 12);
	}
	
	/**
	 * Adds the given note with specified velocity, length, and release velocity to the target
	 * track.
	 * 
	 * @param trackNumber the target track
	 * @param note the note to be added (0-127)
	 * @param startVelocity the initial velocity of the note (0-127)
	 * @param length the length of the note as a multiple of a quarter note
	 * (ex. 4.0 = whole note, 0.5 = eighth note)
	 * @param releaseVelocity the release velocity of the note (0-127)
	 * @param timestamp the timestamp of the message, in ticks
	 * 
	 * @throws InvalidMidiDataException
	 */
	public void addNote(int trackNumber, int note, int startVelocity, double length,
	        int releaseVelocity, long timestamp) throws InvalidMidiDataException {
	    ShortMessage noteOn = new ShortMessage(ShortMessage.NOTE_ON, note, startVelocity);
	    addMessage(trackNumber, noteOn, timestamp);
	    long lengthInTicks = (long) (length * getQuarterNoteLength());
	    ShortMessage noteOff = new ShortMessage(ShortMessage.NOTE_OFF, note, releaseVelocity);
	    addMessage(trackNumber, noteOff, timestamp + lengthInTicks);
	}
	
	 /**
     * Adds the given note with specified velocity, length, and release velocity to the target
     * track.
     * 
     * @param trackNumber the target track
     * @param note the note to be added
     * @param startVelocity the initial velocity of the note (0-127)
     * @param length the length of the note as a multiple of a quarter note
     * (ex. 4.0 = whole note, 0.5 = eighth note)
     * @param releaseVelocity the release velocity of the note (0-127)
     * @param timestamp the timestamp of the message, in ticks
     * 
     * @throws InvalidMidiDataException
     */
	public void addNote(int trackNumber, String note, int startVelocity, double length,
	        int releaseVelocity, long timestamp) throws InvalidMidiDataException {
	    int noteNum = noteStringToInt(note);
	    addNote(trackNumber, noteNum, startVelocity, length, releaseVelocity, timestamp);
	}
	
	/**
	 * Signifies the end of the target track.
	 * 
	 * @param trackNumber the target track
	 * @param timestamp the timestamp of the message, in ticks
	 * 
	 * @throws InvalidMidiDataException
	 */
	public void setEndOfTrack(int trackNumber, long timestamp) throws InvalidMidiDataException {
	    byte[] data = {}; //empty array to signal end of track
	    MetaMessage message = new MetaMessage(0x2F, data, 0);
	    addMessage(trackNumber, message, timestamp);
	}
	
	/**
	 * Writes the sequence to a type 1 MIDI file.
	 * 
	 * @param fileName name of the resulting file
	 * 
	 * @return a {@code File} containing the MIDI data
	 * 
	 * @throws IOException
	 */
	public File writeFile(String fileName) throws IOException {
	    File file = new File(fileName + ".mid");
	    MidiSystem.write(sequence, 1, file);
	    return file;
	}
	
	/**
	 * Plays the current sequence.
	 * 
	 * @throws MidiUnavailableException
	 * @throws InvalidMidiDataException
	 * @throws InterruptedException
	 */
	public void playSequence() throws MidiUnavailableException, InvalidMidiDataException,
			InterruptedException {
		Sequencer sequencer = MidiSystem.getSequencer();
		sequencer.setSequence(sequence);
		sequencer.open();
		Thread.sleep(500);
		sequencer.start();
		sequencer.addMetaEventListener(new MetaEventListener() {
			//close the sequencer when track is done playing
			public void meta(MetaMessage message) {
				if(message.getType() == 0x2F) { //if we find the end of track message
					sequencer.close();
				}
			}
		});
	}
}
