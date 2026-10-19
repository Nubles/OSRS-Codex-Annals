package com.codex.annals.ui;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A search box in the panel's own ink and gold: a well with a small glass, a faint prompt while
 * empty, and a pause after typing before it asks for the timeline to be filtered, so a long
 * history is not rebuilt at every key. Escape clears it.
 */
class SearchField extends JTextField
{
	private static final int H = 24;
	private final String prompt;

	SearchField(String prompt, Runnable changed)
	{
		this.prompt = prompt;
		setOpaque(false);
		setFont(Theme.body());
		setForeground(Theme.TEXT);
		setCaretColor(Theme.GOLD);
		setSelectionColor(Theme.alpha(Theme.GOLD, 90));
		setSelectedTextColor(Theme.TEXT);
		setBorder(new EmptyBorder(3, 24, 3, 8));
		setPreferredSize(new Dimension(10, H));
		setToolTipText("Find entries by name, skill, boss or item");
		Timer pause = new Timer(250, e -> changed.run());
		pause.setRepeats(false);
		getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				pause.restart();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				pause.restart();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				pause.restart();
			}
		});
		addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					setText("");
				}
			}
		});
	}

	/** What to look for, trimmed and lower case; empty for nothing. */
	String query()
	{
		return getText() == null ? "" : getText().trim().toLowerCase(java.util.Locale.ENGLISH);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		RoundRectangle2D well = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 8, 8);
		g.setColor(Theme.INK_DEEP);
		g.fill(well);
		g.setColor(hasFocus() ? Theme.alpha(Theme.GOLD, 160) : Theme.STROKE);
		g.draw(well);
		// The glass.
		g.setColor(hasFocus() || !getText().isEmpty() ? Theme.GOLD : Theme.GOLD_DIM);
		g.setStroke(new java.awt.BasicStroke(1.4f));
		g.draw(new Ellipse2D.Float(8, h / 2f - 5, 8, 8));
		g.drawLine(15, h / 2 + 2, 18, h / 2 + 5);
		if (getText().isEmpty())
		{
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			g.setColor(Theme.TEXT_FAINT);
			g.drawString(prompt, 24, (h + fm.getAscent()) / 2 - 1);
		}
		g.dispose();
		super.paintComponent(graphics);
	}
}
