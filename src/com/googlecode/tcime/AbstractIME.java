/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.tcime;

import android.content.Context;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/**
 * Abstract class extended by ZhuyinIME and CangjieIME.
 */
public abstract class AbstractIME extends InputMethodService implements 
    KeyboardView.OnKeyboardActionListener, CandidateView.CandidateViewListener {

  protected SoftKeyboardView inputView;
 // private CandidatesContainer candidatesContainer;
  public CandidatesContainer candidatesContainer;//sunfabo
  private KeyboardSwitch keyboardSwitch;
  private Editor editor;
  private WordDictionary wordDictionary;
  private PhraseDictionary phraseDictionary;
  private SoundMotionEffect effect;
  private int orientation;
  
  protected abstract KeyboardSwitch createKeyboardSwitch(Context context);
  protected abstract Editor createEditor();
  protected abstract WordDictionary createWordDictionary(Context context);
  public SoftKeyboardView   mInputView; 
  @Override
  public void onCreate() {
    super.onCreate();
    Log.d("AbstractIME","onCreate()>>>");
    keyboardSwitch = createKeyboardSwitch(this);
    editor = createEditor();
    wordDictionary = createWordDictionary(this);
    phraseDictionary = new PhraseDictionary(this);
    effect = new SoundMotionEffect(this);
    orientation = getResources().getConfiguration().orientation;
    // Use the following line to debug IME service.
    //android.os.Debug.waitForDebugger();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    if (orientation != newConfig.orientation) {
      // Clear composing text and candidates for orientation change.
      Log.d("AbstractIME","onConfigurationChanged()>>>");
      escape();
      orientation = newConfig.orientation;
    }
    super.onConfigurationChanged(newConfig);
  }

  @Override
  public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart,
      int newSelEnd, int candidatesStart, int candidatesEnd) {
    super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, 
        candidatesStart, candidatesEnd);
    Log.d("AbstractIME","onUpdateSelection()>>>");

    if ((candidatesEnd != -1) &&
        ((newSelStart != candidatesEnd) || (newSelEnd != candidatesEnd))) {
      // Clear composing text and its candidates for cursor movement.
      escape();
    }
    // Update the caps-lock status for the current cursor position.
    updateCursorCapsToInputView();
  }

  @Override
  public void onComputeInsets(InputMethodService.Insets outInsets) {
    super.onComputeInsets(outInsets);
    Log.d("AbstractIME","onComputeInsets()>>>");
    outInsets.contentTopInsets = outInsets.visibleTopInsets;
  }

  @Override
  public View onCreateInputView() {
	Log.d("AbstractIME","onCreateInputView()>>>");
  
    inputView = (SoftKeyboardView) getLayoutInflater().inflate(
        R.layout.input, null);
    inputView.setOnKeyboardActionListener(this);
    mInputView=inputView;
    return inputView;
  }

  @Override
  public View onCreateCandidatesView() {
	Log.d("AbstractIME","onCreateCandidatesView()>>>");

    candidatesContainer = (CandidatesContainer) getLayoutInflater().inflate(
        R.layout.candidates, null);
    candidatesContainer.setCandidateViewListener(this);
    return candidatesContainer;
  }
  @Override
  public void onStartInputView(EditorInfo attribute, boolean restarting) {
    super.onStartInputView(attribute, restarting);
    // Reset editor and candidates when the input-view is just being started.
	Log.d("AbstractIME","onStartInputView()>>>");

    editor.start(attribute.inputType);
    clearCandidates();
    effect.reset();

    keyboardSwitch.initializeKeyboard(getMaxWidth());
    // Select a keyboard based on the input type of the editing field.
    keyboardSwitch.onStartInput(attribute.inputType);
    bindKeyboardToInputView();
  }

  @Override
  public void onFinishInput() {
    // Clear composing as any active composing text will be finished, same as in
    // onFinishInputView, onFinishCandidatesView, and onUnbindInput.
	Log.d("AbstractIME","onFinishInput()>>>");
  
    editor.clearComposingText(getCurrentInputConnection());
    super.onFinishInput();
  }

  @Override
  public void onDestroy() {
    super. onDestroy();
	Log.d("AbstractIME","onDestroy()>>>");

  }
  
  @Override
  public void onFinishInputView(boolean finishingInput) {
	Log.d("AbstractIME","onFinishInputView()>>>");
    editor.clearComposingText(getCurrentInputConnection());
    super.onFinishInputView(finishingInput);
    // Dismiss any pop-ups when the input-view is being finished and hidden.
    inputView.closing(); 
  }

  @Override
  public void onFinishCandidatesView(boolean finishingInput) {
	  Log.d("AbstractIME","onFinishCandidatesView()>>>");
    editor.clearComposingText(getCurrentInputConnection());
    super.onFinishCandidatesView(finishingInput);
  }

  @Override
  public void onUnbindInput() {
	  Log.d("AbstractIME","onUnbindInput()>>>");  
    editor.clearComposingText(getCurrentInputConnection());
    super.onUnbindInput();
  }

  private void bindKeyboardToInputView() {
	Log.d("AbstractIME","bindKeyboardToInputView()>>>");  
    if (inputView != null) {
      // Bind the selected keyboard to the input view.
      inputView.setKeyboard(keyboardSwitch.getCurrentKeyboard());
      updateCursorCapsToInputView();
    }
  }
 
  private void updateCursorCapsToInputView() {
	Log.d("AbstractIME","updateCursorCapsToInputView()>>>");  

    InputConnection ic = getCurrentInputConnection();
    if ((ic != null) && (inputView != null)) {
      int caps = 0;
      EditorInfo ei = getCurrentInputEditorInfo();
      if ((ei != null) && (ei.inputType != EditorInfo.TYPE_NULL)) {
        caps = ic.getCursorCapsMode(ei.inputType);
      }
      inputView.updateCursorCaps(caps);
    }
  }

  private void commitText(CharSequence text) {
	  Log.d("AbstractIME","commitText()>>>");  

    if (editor.commitText(getCurrentInputConnection(), text)) {
      // Clear candidates after committing any text.
      clearCandidates();
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {	
	Log.d("AbstractIME","onKeyDown()>>>");  

	if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getRepeatCount() == 0)) {
		// Handle the back-key to close the pop-up keyboards.
		if ((inputView != null) && inputView.handleBack()) {
		  return true;
		}
	}
	    
    return super.onKeyDown(keyCode, event);
  }

  public void onKey(int primaryCode, int[] keyCodes) {
	  
    if (keyboardSwitch.onKey(primaryCode)) {
    	 Log.v("AbstractIME"," keyboardSwitch.onKey(primaryCode)"+ primaryCode);//sunfabo
      escape();
      bindKeyboardToInputView();
      return;
    }
    if (handleOption(primaryCode) || handleCapsLock(primaryCode)
        || handleEnter(primaryCode) || handleSpace(primaryCode)
        || handleDelete(primaryCode) || handleComposing(primaryCode)) {
    	 Log.v("AbstractIME"," handleOption(primaryCode)" + primaryCode);//sunfabo
      return;
    }
    handleKey(primaryCode);
  }

  public void onText(CharSequence text) {
	  Log.v("AbstractIME","onText " + text);//sunfabo
    commitText(text);
  }

  public void onPress(int primaryCode) {
	Log.v("AbstractIME","onPress(primaryCode) " + primaryCode);//sunfabo
    effect.vibrate();
    effect.playSound();
  }

  public void onRelease(int primaryCode) {
	Log.v("AbstractIME","onRelease(primaryCode) " + primaryCode);
    // no-op
  }

  public void swipeLeft() {
    // no-op
	  Log.v("AbstractIME","swipeLeft");
  }

  public void swipeRight() {
    // no-op
	  Log.v("AbstractIME","swipeRight");
  }

  public void swipeUp() {
	  Log.v("AbstractIME","swipeUp");
    // no-op
  }

  public void swipeDown() {
	 Log.v("AbstractIME","swipeDown");
    requestHideSelf(0);
  }

  public void onPickCandidate(String candidate) {
    // Commit the picked candidate and suggest its following words.
    commitText(candidate);
    setCandidates(phraseDictionary.getFollowingWords(candidate.charAt(0)), false);
    Log.v("AbstractIME"," onPickCandidate(String candidate)"+ candidate);
    
  }

  private void clearCandidates() {//清除候选区
    setCandidates("", false);
    Log.v("AbstractIME","clearCandidates() ");
  }

  private void setCandidates(String words, boolean highlightDefault) {
	Log.v("AbstractIME","setCandidates() words " + words + "highlightDefault" + highlightDefault);
    if (candidatesContainer != null) {
      candidatesContainer.setCandidates(words, highlightDefault);
      setCandidatesViewShown((words.length() > 0) || editor.hasComposingText());
      if (inputView != null) {
        inputView.setEscape(candidatesContainer.isShown());
      }
    }
  }

  private boolean handleOption(int keyCode) {
	 Log.v("AbstractIME"," handleOption(int keyCode)"+ keyCode); 
    if (keyCode == SoftKeyboard.KEYCODE_OPTIONS) {
      // TODO: Do voice input here.
      return true;
    }
    return false;
  }

  private boolean handleCapsLock(int keyCode) {
    return (keyCode == Keyboard.KEYCODE_SHIFT) && inputView.toggleCapsLock();
  }

  private boolean handleEnter(int keyCode) {
	  
    if (keyCode == '\n') {
      if (inputView.hasEscape()) {
        escape();
      } else if (editor.treatEnterAsLinkBreak()) {
        commitText("\n");
      } else {
        sendKeyChar('\n');
      }
      return true;
    }
    return false;
  }

  private boolean handleSpace(int keyCode) {
    if (keyCode == ' ') {
      if ((candidatesContainer != null) && candidatesContainer.isShown()) {
        // The space key could either pick the highlighted candidate or escape
        // if there's no highlighted candidate and no composing-text.
        if (!candidatesContainer.pickHighlighted()
            && !editor.hasComposingText()) {
          escape();
        }
      } else {
        commitText(" ");
      }
      return true;
    }
    return false;
  }

  private boolean handleDelete(int keyCode) {
    // Handle delete-key only when no composing text. 
    if ((keyCode == Keyboard.KEYCODE_DELETE) && !editor.hasComposingText()) {
      if (inputView.hasEscape()) {
        escape();
      } else {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
      }
      return true;
    }
    return false;
  }

  private boolean handleComposing(int keyCode) {
    if (editor.compose(getCurrentInputConnection(), keyCode)) {
      // Set the candidates for the updated composing-text and provide default
      // highlight for the word candidates.
      setCandidates(wordDictionary.getWords(editor.composingText()), true);
      return true;
    }
    return false;
  }

  /**
   * Handles input of SoftKeybaord key code that has not been consumed by
   * other handling-methods.
   */
  private void handleKey(int keyCode) {
    if (isInputViewShown() && inputView.isShifted()) {
      keyCode = Character.toUpperCase(keyCode);
      Log.v("AbstractIME","handleKey(int keyCode) keyCode = Character.toUpperCase(keyCode);" + keyCode);
    }
    commitText(String.valueOf((char) keyCode));
  }

  /**
   * Simulates PC Esc-key function by clearing all composing-text or candidates.
   */
  protected void escape() {
	 
    editor.clearComposingText(getCurrentInputConnection());
    clearCandidates();
  }
}
