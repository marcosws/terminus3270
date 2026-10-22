package com.github.marcosws.terminus3270.components;

/**
 * Represents the position of the cursor on the 3270 screen.
 * The row and column are zero-based indices.
 */
public class CursorPosition {
	
	private final int row;
	private final int col;
	
	/**
	 * CursorPosition(int row, int col)
	 * @param row
	 * @param col
	 */
	public CursorPosition(int row, int col) {
		if (row < 0 || col < 0) {
			throw new IllegalArgumentException(
					"Cursor position cannot be negative: row=" + row + ", col=" + col
			);
		}
		this.row = row;
		this.col = col;
	}
	
	public int getRow() {
		return row;
	}
	
	public int getCol() {
		return col;
	}
	
	public CursorPosition offset(int dRow, int dCol) {
		return new CursorPosition(this.row + dRow, this.col + dCol);
	}
	
	@Override
	public String toString() {
		return "CursorPosition[row=" + row + ", col=" + col + "]";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) 
			return true;
		if (!(obj instanceof CursorPosition)) 
			return false;
		CursorPosition other = (CursorPosition) obj;
		return row == other.row && col == other.col;
	}
	
	@Override
	public int hashCode() {
		return 31 * row + col;
	}

}
