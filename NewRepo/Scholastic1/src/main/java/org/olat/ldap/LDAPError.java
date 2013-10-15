package org.olat.ldap;

import java.io.Serializable;

public class LDAPError implements Serializable {

	private ErrorNode head, end;
	private int size;

	public LDAPError() {
		head = new ErrorNode(null, null, end);
		end = new ErrorNode(null, head, null);
		size = 0;
	}

	public boolean isEmpty() {
		return (size == 0) ? true : false;
	}

	public int size() {
		return this.size;
	}

	public void insert(final String error) {
		final ErrorNode newError = new ErrorNode(error, end.getPrev(), end);
		end.getPrev().setNext(newError);
		end.setPrev(newError);
		size++;
	}

	public String get() {
		if (isEmpty() != true) {
			final ErrorNode errorNode = end.getPrev();
			final String error = errorNode.getError();
			errorNode.getPrev().setNext(end);
			end.setPrev(errorNode.getPrev());
			size--;
			return error;
		} else {
			return null;
		}
	}

	public class ErrorNode implements Serializable {

		private String error;
		private ErrorNode next, prev;

		public ErrorNode() {
			this(null, null, null);
		}

		public ErrorNode(final String error) {
			this(error, null, null);
		}

		public ErrorNode(final String error, final ErrorNode prev, final ErrorNode next) {
			this.error = error;
			this.next = next;
			this.prev = prev;
		}

		public void setError(final String error) {
			this.error = error;
		}

		public void setNext(final ErrorNode next) {
			this.next = next;
		}

		public void setPrev(final ErrorNode prev) {
			this.prev = prev;
		}

		public ErrorNode getNext() {
			return this.next;
		}

		public ErrorNode getPrev() {
			return this.prev;
		}

		public String getError() {
			return this.error;
		}
	}

}
