# MusicGenerator
Simple random music generator written in Java. Generates MIDI files, each about a minute long.

Run from RandomMain. You can find pre-generated MIDI files in the examples folder.

Uses the javax.sound.midi library and a little bit of music theory knowledge. Certain quantities are given weights: things like notes in a key, length of notes, which octaves those notes are in, with higher weights given to things that sound "good". Feel free to play around with tweaking these weights and seeing how the generator performs differently.
