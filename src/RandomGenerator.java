import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;

public class RandomGenerator {
	
	private MidiWriter writer;
	private static Random rng = new Random();
	
	//below are randomly generated
	private int length; //length in ticks, minimum length of 64 quarter ntoes
	private int key; //number between -4 and 8 added to each note
	private int bpm; //between 100-200 beats per minute
	
									 //C  D  E  F  G  A  B		
	static final int[] NOTES_IN_KEY = {0, 2, 4, 5, 7, 9, 11};
	                                 //C   C# D  D# E  F  F# G  G# A   A# B
	static final int[] NOTE_WEIGHTS = {10, 4, 8, 4, 7, 9, 4, 9, 4, 10, 4, 6};
	//describes the probability of a certain note appearing
	static final double[] LENGTHS = {0.25, 0.5, 0.75, 1, 1.5, 2, 3, 4, 6, 7, 8};
	static final int[] CHORD_LENGTH_WEIGHTS = {1, 2, 1, 5, 2, 7, 6, 8, 7, 4, 5};
	//describes the possibility of having a chord of the given length
	static final int[] BASS_NUMBER_NOTES_WEIGHTS = {1, 2, 4, 5, 3, 2};
	//describes probability of having a certain amount of notes in the bassline
	static final int[] BASS_OCTAVE_WEIGHTS = {1, 1, 2, 5, 4, 3, 2, 1, 1, 1};
	//describes possibilty of note in bass being at a certain octave
	static final int[] TREBLE_LENGTH_WEIGHTS = {7, 9, 4, 10, 6, 8, 5, 6, 4, 2, 3};
	static final int[] TREBLE_OCTAVE_WEIGHTS = {1, 1, 1, 2, 3, 5, 4, 3, 1, 1};
	static final int[] TREBLE_NUMBER_NOTES_WEIGHTS = {5, 4, 2, 1, 1};
	static final int[] VELOCITY_WEIGHTS = {1, 3, 5, 1};
	//describes probability of note of certain velocity occuring: either quiet (0-40), medium
	//(40-60), loud (60-90) or very loud (90-127)
	static final int TIMING_RESOLUTION = 24;
	
	public RandomGenerator() {
		try {
            double lengthProbability = 1.0;
            length = 64 * TIMING_RESOLUTION;
            while(rng.nextDouble() < lengthProbability) {
                length += 16 * TIMING_RESOLUTION;
                lengthProbability *= 0.9;
                //randomly pick a length
            }
            key = rng.nextInt(13) - 4; //random key 
            bpm = rng.nextInt(110) + 90; //random tempo between 90 bpm and 199 bpm
            //create sequence with 24 ticks per quarter note
            init();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void init() throws InvalidMidiDataException {
	    //initializes MidiWriter
        writer = new MidiWriter(Sequence.PPQ, TIMING_RESOLUTION);
        writer.createTrack();
        writer.enableGeneralMidiSoundset(0);
        writer.setTrackName(0, "SBAC");
        writer.setBPM(0, bpm, 0);
        writer.setOmni(0, true);
        writer.setPoly(0, true);
        writer.setInstrument(0, 0); //instrument 0 is piano
	}
	
	public void writeSequence() throws InvalidMidiDataException {
		//generate loose chord sequence for track to follow
		List<Chord> chords = generateChordSequence();
		
		long timestamp = 0;
		for(Chord c : chords) {
		    generateBass(c, timestamp);
		    generateMelody(c, timestamp);
			timestamp += c.getLength() * TIMING_RESOLUTION;
		}
		
		writer.setEndOfTrack(0, timestamp + 4 * TIMING_RESOLUTION);
	}
	
	private List<Chord> generateChordSequence() {
		//generate a loose chord structure for track to follow
		double beat = 0.0;
		int prevNote = -1;
		double noteLength = 0;
		List<Chord> chords = new ArrayList<Chord>();
		
		while(beat * TIMING_RESOLUTION < length) {
		    int[] noteMultipliers = new int[NOTE_WEIGHTS.length];
			for(int i = 0; i < noteMultipliers.length; i++) {
                noteMultipliers[i] = NOTE_WEIGHTS[i];
                if(i == prevNote) {
                    noteMultipliers[i] -= 2; //reduce chance of two consecutive chords
                }
            }
            
		    int[] lengthMultipliers = new int[CHORD_LENGTH_WEIGHTS.length];
            for(int i = 0; i < lengthMultipliers.length; i++) {
                lengthMultipliers[i] = CHORD_LENGTH_WEIGHTS[i];
                if((LENGTHS[i] + beat) % 4 == 0) {
                    lengthMultipliers[i] += 5; //increase chance of chord ending on a bar
                }
            }
                
			prevNote = getRandomizedMax(noteMultipliers);
			noteLength = LENGTHS[getRandomizedMax(lengthMultipliers)];
			Chord chord = new Chord(prevNote, noteLength);
			//create a Note to hold relative value of note and length; not intended to be played
			chords.add(chord);
			beat += noteLength;
		}	
		
		return chords;
	}
	
	public void playSequence() throws MidiUnavailableException, InvalidMidiDataException,
			InterruptedException {
		writer.playSequence();
	}
	
	private List<Integer> getChord(int note) {
		List<Integer> notesInChord = new ArrayList<Integer>();
		notesInChord.add(note); //base note will always be in chord

		if(Arrays.binarySearch(NOTES_IN_KEY, note) != -1) {
			notesInChord.add((NOTES_IN_KEY[(Arrays.binarySearch(NOTES_IN_KEY, note) + 2) % 7]));
			//if the note is in key, add the third
			//third is located 2 indices above the note in NOTES_IN_KEY array
		}
		
		notesInChord.add((note + 7) % 12); //tonic will always be in chord
		
		return notesInChord;
	}
	
	private int getMax(double[] nums) {
		int index = 0;
		double max = nums[0];
		for(int i = 1; i < nums.length; i++) {
			if(nums[i] > max) {
				index = i;
				max = nums[i];
			}
		}
		return index;
	}
	
	//returns a randomized array based on weighted array
	private double[] getRandomizedArray(int[] weights) {
		double[] randomized = new double[weights.length];
		for(int i = 0; i < randomized.length; i++) {
			randomized[i] = rng.nextDouble() * weights[i];
		}
		
		return randomized;
	}
	
	private int getRandomizedMax(int[] weights) {
		return getMax(getRandomizedArray(weights));
	}
	
	private void generateBass(Chord chord, long timestamp) throws InvalidMidiDataException {
	    generateChord(chord, chord.getLength(), BASS_OCTAVE_WEIGHTS, BASS_NUMBER_NOTES_WEIGHTS,
	            timestamp);
	}
	
	private void generateMelody(Chord chord, long timestamp) throws InvalidMidiDataException {
	    long currentTimestamp = timestamp;
	    while(currentTimestamp < chord.getLength() * TIMING_RESOLUTION + timestamp) {
	        double length = LENGTHS[getRandomizedMax(TREBLE_LENGTH_WEIGHTS)];
	        generateChord(chord, length, TREBLE_OCTAVE_WEIGHTS, TREBLE_NUMBER_NOTES_WEIGHTS,
	                currentTimestamp);
	        currentTimestamp += length * TIMING_RESOLUTION;
	    }
	}
	
	private void generateChord(Chord chord, double length, int[] octaveWeights,
	        int[] numberNotesWeights, long timestamp) throws InvalidMidiDataException {
	    int numNotes = getRandomizedMax(numberNotesWeights);
	    
	    int[] noteWeights = Arrays.copyOf(NOTE_WEIGHTS, NOTE_WEIGHTS.length);
	    List<Integer> notesInChord = getChord(chord.getNote());
	    for(Integer i : notesInChord) {
	        noteWeights[i] += 5; //increase chance of notes in chord appearing
	    }
	    
	    int velocityRange = getRandomizedMax(VELOCITY_WEIGHTS);
	    int velocity; //all notes in chord will have the same velocity
	    if(velocityRange == 0) {
	        velocity = rng.nextInt(40) + 1;
	    } else if(velocityRange == 1) {
	        velocity = rng.nextInt(20) + 41;
	    } else if(velocityRange == 2) {
	        velocity = rng.nextInt(30) + 61;
	    } else {
	        velocity = rng.nextInt(37) + 91;
	    }
	    
	    for(int i = 0; i < numNotes; i++) {
            int note = getRandomizedMax(noteWeights);
            int octave = getRandomizedMax(octaveWeights);
            int noteMidiCode = octave * 12 + note + key;
            writer.addNote(0, noteMidiCode, velocity, length, 0, timestamp);
        }
	}
	
	public int getLengthInMilliseconds() {
	    return (int) ((1.0 / bpm) * (length / TIMING_RESOLUTION) * 60000);
	}
	
	public File saveSequence(String fileName) throws IOException {
	    return writer.writeFile(fileName);
	}
	
	private class Chord {
	    //local class to hold two values
	    private int note; //0-11
	    private double length; //multiple of quarter note; ex. 0.5 = eighth note, 2.0 = half note
	    
	    public Chord(int note, double length) {
	        this.note = note;
	        this.length = length;
	    }
	    
	    public int getNote() { return note; }
	    public double getLength() { return length; }
	}
}
