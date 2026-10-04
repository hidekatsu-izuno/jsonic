function renderMemoTable(memos) {
	var table = $('<table>');
	table.append('<tr><th width="30">ID</th><th width="50">Title</th><th width="300">Text</th><th width="50">&nbsp;</th></tr>');
	memos.forEach(function(memo) {
		var row = $('<tr>').appendTo(table);
		$('<td>').text(memo.id).appendTo(row);
		$('<input>', {id: 'title' + memo.id, type: 'text'})
			.css('width', '100%').val(memo.title).appendTo($('<td>').appendTo(row));
		$('<input>', {id: 'text' + memo.id, type: 'text'})
			.css('width', '100%').val(memo.text).appendTo($('<td>').appendTo(row));
		var actions = $('<td>').appendTo(row);
		$('<input>', {id: 'update' + memo.id, type: 'button', value: 'Update'})
			.on('click', function() { updateRow(memo.id); }).appendTo(actions);
		$('<input>', {id: 'delete' + memo.id, type: 'button', value: 'Delete'})
			.on('click', function() { deleteRow(memo.id); }).appendTo(actions);
	});
	var row = $('<tr>').appendTo(table);
	$('<td>').text('\u00a0').appendTo(row);
	$('<input>', {id: 'title', type: 'text'})
		.css('width', '100%').appendTo($('<td>').appendTo(row));
	$('<input>', {id: 'text', type: 'text'})
		.css('width', '100%').appendTo($('<td>').appendTo(row));
	$('<input>', {id: 'add', type: 'button', value: 'Add'})
		.on('click', addRow).appendTo($('<td>').appendTo(row));
	$('#list').empty().append(table);
}
